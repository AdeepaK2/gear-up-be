"""RAG (Retrieval-Augmented Generation) Service"""

import logging
import time
import uuid
import os
from typing import List, Dict, Any, Optional, AsyncGenerator
from datetime import datetime

from app.services.gemini_service import GeminiService
from app.services.vector_db_service import VectorDBService
from app.services.appointment_service import appointment_service
from app.models.schemas import ChatResponse, ChatStreamChunk, ChatMessage
from app.database.chat_history_db import (
    save_chat_message,
    get_chat_history as db_get_chat_history,
    clear_chat_history as db_clear_chat_history
)

logger = logging.getLogger(__name__)


class RAGService:
    """Service for RAG-based question answering"""
    
    def __init__(self, gemini_service: GeminiService, vector_db_service: VectorDBService):
        self.gemini_service = gemini_service
        self.vector_db_service = vector_db_service
        self.max_context_docs = int(os.getenv("MAX_CONTEXT_DOCS", "5"))
    
    async def process_query(
        self,
        question: str,
        session_id: Optional[str] = None,
        conversation_history: Optional[List[ChatMessage]] = None,
        filters: Optional[Dict[str, Any]] = None,
        customer_id: Optional[int] = None,
        customer_email: Optional[str] = None,
        auth_token: Optional[str] = None
    ) -> ChatResponse:
        """
        Process user query with RAG
        
        Args:
            question: User question
            session_id: Session identifier
            conversation_history: Previous conversation messages
            filters: Additional filters for context retrieval
            customer_id: Customer ID for appointment queries
            customer_email: Customer email for appointment queries
            auth_token: JWT token for API authentication
        
        Returns:
            ChatResponse with answer and metadata
        """
        start_time = time.time()
        session_id = session_id or str(uuid.uuid4())
        
        try:
            # Check if this is an appointment-related query
            appointment_keywords = [
                "appointment", "appointments", "scheduled", "booking", "book",
                "available", "upcoming", "when is my", "what appointments",
                "do i have", "schedule", "slots", "meeting"
            ]
            
            question_lower = question.lower()
            is_appointment_query = any(keyword in question_lower for keyword in appointment_keywords)
            
            if is_appointment_query:
                logger.info(f"Processing appointment query: {question[:50]}...")
                
                # Handle appointment-specific query
                appointment_response = await appointment_service.process_appointment_query(
                    query=question,
                    customer_id=customer_id,
                    customer_email=customer_email,
                    auth_token=auth_token
                )
                
                # Calculate processing time
                processing_time = int((time.time() - start_time) * 1000)
                
                # Store in chat history
                await self._save_to_history(session_id, question, appointment_response)
                
                return ChatResponse(
                    answer=appointment_response,
                    session_id=session_id,
                    from_cache=False,
                    processing_time_ms=processing_time,
                    timestamp=datetime.utcnow(),
                    confidence=0.9,  # High confidence for direct API calls
                    sources=["appointment_api"]
                )
            
            # Step 1: Retrieve relevant context from vector DB
            logger.info(f"Retrieving context for: {question[:50]}...")
            relevant_docs = await self.vector_db_service.search(
                query=question,
                top_k=self.max_context_docs,
                filters=self._build_filters(filters)
            )
            
            # Step 2: Build context from retrieved documents
            context = self._build_context(relevant_docs)
            sources = [doc["metadata"].get("source", "") for doc in relevant_docs]
            
            # Step 3: Generate response using Gemini
            logger.info("Generating response with Gemini...")
            history = self._format_history(conversation_history) if conversation_history else None
            
            answer = await self.gemini_service.generate_response(
                prompt=question,
                context=context,
                conversation_history=history
            )
            
            # Calculate processing time
            processing_time = int((time.time() - start_time) * 1000)
            
            # Calculate confidence based on relevance scores
            confidence = self._calculate_confidence(relevant_docs)
            
            # Store in chat history (async)
            await self._save_to_history(session_id, question, answer)
            
            return ChatResponse(
                answer=answer,
                session_id=session_id,
                from_cache=False,
                processing_time_ms=processing_time,
                timestamp=datetime.utcnow(),
                confidence=confidence,
                sources=sources
            )
        
        except Exception as e:
            logger.error(f"Error processing query: {e}", exc_info=True)
            return ChatResponse(
                answer="I apologize, but I'm having trouble processing your request. Please try again.",
                session_id=session_id,
                from_cache=False,
                processing_time_ms=int((time.time() - start_time) * 1000),
                timestamp=datetime.utcnow(),
                confidence=0.0,
                sources=[]
            )
    
    async def process_query_stream(
        self,
        question: str,
        session_id: Optional[str] = None,
        conversation_history: Optional[List[ChatMessage]] = None,
        filters: Optional[Dict[str, Any]] = None
    ) -> AsyncGenerator[ChatStreamChunk, None]:
        """
        Process user query with streaming RAG response
        
        Yields:
            ChatStreamChunk objects with incremental content
        """
        session_id = session_id or str(uuid.uuid4())
        chunk_index = 0
        
        try:
            # Step 1: Retrieve context (same as non-streaming)
            logger.info(f"Retrieving context for stream: {question[:50]}...")
            relevant_docs = await self.vector_db_service.search(
                query=question,
                top_k=self.max_context_docs,
                filters=self._build_filters(filters)
            )
            
            context = self._build_context(relevant_docs)
            history = self._format_history(conversation_history) if conversation_history else None
            
            # Step 2: Stream response from Gemini
            logger.info("Streaming response with Gemini...")
            async for chunk_text in self.gemini_service.generate_response_stream(
                prompt=question,
                context=context,
                conversation_history=history
            ):
                yield ChatStreamChunk(
                    content=chunk_text,
                    is_final=False,
                    session_id=session_id,
                    chunk_index=chunk_index
                )
                chunk_index += 1
            
            # Send final marker
            yield ChatStreamChunk(
                content="",
                is_final=True,
                session_id=session_id,
                chunk_index=chunk_index
            )
        
        except Exception as e:
            logger.error(f"Error streaming query: {e}", exc_info=True)
            yield ChatStreamChunk(
                content="I apologize, but I'm having trouble processing your request.",
                is_final=True,
                session_id=session_id,
                chunk_index=0
            )
    
    async def get_statistics(self) -> Dict[str, Any]:
        """Get RAG service statistics"""
        return {
            "vector_db_type": self.vector_db_service.vector_db_type,
            "max_context_docs": self.max_context_docs,
            "gemini_available": self.gemini_service.is_available(),
            "vector_db_available": self.vector_db_service.is_available(),
        }
    
    async def get_chat_history(self, session_id: str, limit: int = 10) -> List[Dict[str, Any]]:
        """
        Get chat history for a session from database
        
        Args:
            session_id: Session identifier
            limit: Maximum number of messages to return
        
        Returns:
            List of chat messages
        """
        return await db_get_chat_history(session_id, limit)
    
    async def clear_chat_history(self, session_id: str) -> None:
        """
        Clear chat history for a session from database
        
        Args:
            session_id: Session identifier
        """
        await db_clear_chat_history(session_id)
    
    # Private helper methods
    
    async def _save_to_history(self, session_id: str, question: str, answer: str) -> None:
        """Save question and answer to database"""
        try:
            await save_chat_message(session_id, question, answer)
        except Exception as e:
            logger.error(f"Failed to save chat history: {e}")
    
    def _build_context(self, documents: List[Dict[str, Any]]) -> str:
        """Build context string from retrieved documents"""
        if not documents:
            return "No relevant information found."
        
        context_parts = []
        for i, doc in enumerate(documents, 1):
            text = doc.get("text", "")
            score = doc.get("score", 0.0)
            context_parts.append(f"[{i}] (Relevance: {score:.2f})\n{text}")
        
        return "\n\n".join(context_parts)
    
    def _build_filters(self, filters: Optional[Dict[str, Any]]) -> Optional[Dict[str, Any]]:
        """Build metadata filters for vector search"""
        if not filters:
            return None
        
        db_filters = {}
        
        if filters.get("appointment_date"):
            db_filters["date"] = filters["appointment_date"]
        
        if filters.get("service_type"):
            db_filters["service_type"] = filters["service_type"]
        
        return db_filters if db_filters else None
    
    def _format_history(self, conversation_history: List[ChatMessage]) -> List[Dict[str, str]]:
        """Format conversation history for Gemini"""
        return [
            {
                "role": msg.role,
                "content": msg.content
            }
            for msg in conversation_history
        ]
    
    def _calculate_confidence(self, documents: List[Dict[str, Any]]) -> float:
        """Calculate confidence score based on document relevance"""
        if not documents:
            return 0.0
        
        # Average of top document scores
        scores = [doc.get("score", 0.0) for doc in documents[:3]]
        return sum(scores) / len(scores) if scores else 0.0
