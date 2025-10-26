"""
FastAPI Chatbot Service with RAG (Retrieval-Augmented Generation)
Uses Google Gemini for LLM and Pinecone/FAISS for vector database
"""

from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
import os
from dotenv import load_dotenv
import logging
from contextlib import asynccontextmanager

# Load environment variables
load_dotenv()

# Import custom modules (will be created)
from app.services.gemini_service import GeminiService
from app.services.vector_db_service import VectorDBService
from app.services.rag_service import RAGService
from app.database.db import get_db_connection
from app.models.schemas import ChatRequest, ChatResponse, ChatStreamChunk

# Configure logging
logging.basicConfig(
    level=logging.INFO if os.getenv("DEBUG", "false").lower() == "true" else logging.WARNING,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

# Global services
gemini_service = None
vector_db_service = None
rag_service = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager"""
    global gemini_service, vector_db_service, rag_service
    
    logger.info("Initializing Chatbot Service...")
    
    # Initialize services
    gemini_service = GeminiService()
    vector_db_service = VectorDBService()
    rag_service = RAGService(gemini_service, vector_db_service)
    
    # Load initial data into vector database
    await vector_db_service.initialize()
    
    logger.info("Chatbot Service initialized successfully")
    
    yield
    
    # Cleanup
    logger.info("Shutting down Chatbot Service...")
    await vector_db_service.close()


# Create FastAPI app
app = FastAPI(
    title="GearUp AI Chatbot Service",
    description="RAG-based chatbot for appointment queries using Gemini LLM",
    version="1.0.0",
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
async def root():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "GearUp AI Chatbot",
        "version": "1.0.0"
    }


@app.get("/health")
async def health_check():
    """Detailed health check"""
    return {
        "status": "healthy",
        "gemini": gemini_service.is_available(),
        "vector_db": vector_db_service.is_available(),
    }


@app.post("/chat")
async def chat(request: ChatRequest) -> ChatResponse:
    """
    Process chat request with RAG
    Returns complete response
    """
    try:
        logger.info(f"Chat request: {request.question[:50]}...")
        
        response = await rag_service.process_query(
            question=request.question,
            session_id=request.sessionId,
            conversation_history=request.conversationHistory,
            filters={
                "appointment_date": request.appointmentDate,
                "service_type": request.serviceType
            }
        )
        
        return response
    
    except Exception as e:
        logger.error(f"Error processing chat request: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/chat/stream")
async def chat_stream(request: ChatRequest):
    """
    Process chat request with streaming response (SSE)
    Returns incremental chunks
    """
    try:
        logger.info(f"Stream chat request: {request.question[:50]}...")
        
        async def generate_stream():
            async for chunk in rag_service.process_query_stream(
                question=request.question,
                session_id=request.sessionId,
                conversation_history=request.conversationHistory,
                filters={
                    "appointment_date": request.appointmentDate,
                    "service_type": request.serviceType
                }
            ):
                yield f"data: {chunk.model_dump_json()}\n\n"
            
            # Send final marker
            yield "data: {\"is_final\": true}\n\n"
        
        return StreamingResponse(
            generate_stream(),
            media_type="text/event-stream"
        )
    
    except Exception as e:
        logger.error(f"Error processing stream chat request: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/embeddings/update")
async def update_embeddings():
    """
    Manually trigger embeddings update from database
    Should be called when appointment data changes
    """
    try:
        logger.info("Manually updating embeddings...")
        await vector_db_service.update_from_database()
        return {"status": "success", "message": "Embeddings updated successfully"}
    
    except Exception as e:
        logger.error(f"Error updating embeddings: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/chat/history/{session_id}")
async def get_chat_history(session_id: str, limit: int = 10):
    """
    Get chat history for a session
    
    Args:
        session_id: The session ID to retrieve history for
        limit: Maximum number of messages to return (default: 10)
    
    Returns:
        List of chat messages with questions and answers
    """
    try:
        logger.info(f"Retrieving chat history for session: {session_id}")
        history = await rag_service.get_chat_history(session_id, limit)
        return history
    
    except Exception as e:
        logger.error(f"Error getting chat history: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/chat/history/{session_id}")
async def clear_chat_history(session_id: str):
    """
    Clear chat history for a session
    
    Args:
        session_id: The session ID to clear history for
    
    Returns:
        Success message
    """
    try:
        logger.info(f"Clearing chat history for session: {session_id}")
        await rag_service.clear_chat_history(session_id)
        return {"status": "success", "message": f"Chat history cleared for session {session_id}"}
    
    except Exception as e:
        logger.error(f"Error clearing chat history: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/stats")
async def get_stats():
    """Get chatbot statistics"""
    try:
        stats = await rag_service.get_statistics()
        return stats
    
    except Exception as e:
        logger.error(f"Error getting stats: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=os.getenv("HOST", "0.0.0.0"),
        port=int(os.getenv("PORT", 8000)),
        reload=os.getenv("DEBUG", "false").lower() == "true"
    )
