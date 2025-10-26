# 🤖 GearUp AI Chatbot Service

Python FastAPI service for RAG-based chatbot using Google Gemini.

## Features

- ✅ Google Gemini LLM integration
- ✅ RAG (Retrieval-Augmented Generation)
- ✅ Vector database (Pinecone/FAISS)
- ✅ Real-time database embeddings
- ✅ Streaming responses (SSE)
- ✅ Async processing
- ✅ Automatic context retrieval

## Quick Start

### 1. Install Dependencies

```bash
pip install -r requirements.txt
```

### 2. Configure Environment

Copy `.env.example` to `.env` and update:

```env
GEMINI_API_KEY=your_key_here
DATABASE_URL=postgresql+asyncpg://user:pass@host/db
USE_FAISS=true
```

### 3. Run Server

```bash
uvicorn main:app --reload
```

Server will start at: http://localhost:8000

## API Endpoints

- `GET /` - Root endpoint
- `GET /health` - Health check
- `POST /chat` - Send chat message
- `POST /chat/stream` - Stream chat response
- `POST /embeddings/update` - Update embeddings
- `GET /stats` - Get statistics

## Documentation

- Interactive API docs: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## Project Structure

```
chatbot-service/
├── app/
│   ├── services/
│   │   ├── gemini_service.py      # Gemini LLM integration
│   │   ├── vector_db_service.py   # Vector database
│   │   └── rag_service.py         # RAG pipeline
│   ├── database/
│   │   └── db.py                  # Database utilities
│   └── models/
│       └── schemas.py             # Pydantic models
├── main.py                        # FastAPI application
├── requirements.txt               # Python dependencies
└── Dockerfile                     # Docker configuration
```

## Vector Database Options

### FAISS (Local - Default)
```env
USE_FAISS=true
FAISS_INDEX_PATH=./data/faiss_index
```

### Pinecone (Cloud)
```env
USE_PINECONE=true
PINECONE_API_KEY=your_key
PINECONE_INDEX_NAME=gearup-chatbot
```

## Testing

```bash
# Test health
curl http://localhost:8000/health

# Test chat
curl -X POST http://localhost:8000/chat \
  -H "Content-Type: application/json" \
  -d '{"question": "What services do you offer?"}'
```

## Docker

```bash
# Build
docker build -t gearup-chatbot .

# Run
docker run -p 8000:8000 --env-file .env gearup-chatbot
```

## License

MIT
