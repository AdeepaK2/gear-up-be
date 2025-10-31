"""Pydantic schemas for request/response validation"""

from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime


class ChatMessage(BaseModel):
    """Individual chat message"""
    role: str
    content: str
    timestamp: Optional[str] = None


class ChatRequest(BaseModel):
    """Chat request from Spring Boot"""
    question: str = Field(..., min_length=1, max_length=1000)
    sessionId: Optional[str] = None
    conversationHistory: Optional[List[ChatMessage]] = None
    appointmentDate: Optional[str] = None
    serviceType: Optional[str] = None
    customerId: Optional[int] = None
    customerEmail: Optional[str] = None
    authToken: Optional[str] = None


class ChatResponse(BaseModel):
    """Chat response to Spring Boot"""
    answer: str
    session_id: str
    from_cache: bool = False
    processing_time_ms: Optional[int] = None
    timestamp: datetime = Field(default_factory=datetime.utcnow)
    confidence: Optional[float] = None
    sources: List[str] = []


class ChatStreamChunk(BaseModel):
    """Streaming response chunk"""
    content: str
    is_final: bool = False
    session_id: str
    chunk_index: int
