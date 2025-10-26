"""
Test script for database-backed chat history
Run this after migrating to database storage
"""

import asyncio
import sys
import os

# Add parent directory to path
sys.path.insert(0, os.path.abspath(os.path.dirname(__file__)))

from dotenv import load_dotenv
from app.database.chat_history_db import (
    init_chat_history_table,
    save_chat_message,
    get_chat_history,
    clear_chat_history,
    get_recent_sessions
)

load_dotenv()


async def test_chat_history():
    """Test chat history database operations"""
    
    print("🔧 Testing Chat History Database Operations\n")
    
    # Test 1: Initialize table
    print("1️⃣ Initializing chat_history table...")
    try:
        await init_chat_history_table()
        print("✅ Table initialized successfully\n")
    except Exception as e:
        print(f"❌ Table initialization failed: {e}\n")
        return
    
    # Test 2: Save messages
    print("2️⃣ Saving test messages...")
    test_session = "test-session-001"
    
    try:
        await save_chat_message(
            session_id=test_session,
            question="What appointments do I have?",
            answer="You have 2 appointments this week."
        )
        await save_chat_message(
            session_id=test_session,
            question="What's the first one?",
            answer="Your first appointment is on 2025-11-06 at 10:00 AM."
        )
        await save_chat_message(
            session_id=test_session,
            question="Who is the mechanic?",
            answer="The mechanic is Parindya Mullegama."
        )
        print("✅ Messages saved successfully\n")
    except Exception as e:
        print(f"❌ Failed to save messages: {e}\n")
        return
    
    # Test 3: Retrieve history
    print("3️⃣ Retrieving chat history...")
    try:
        history = await get_chat_history(test_session, limit=10)
        print(f"✅ Retrieved {len(history)} messages:")
        for i, msg in enumerate(history, 1):
            print(f"\n   Message {i}:")
            print(f"   Q: {msg['question']}")
            print(f"   A: {msg['answer']}")
            print(f"   Time: {msg['timestamp']}")
        print()
    except Exception as e:
        print(f"❌ Failed to retrieve history: {e}\n")
        return
    
    # Test 4: Get recent sessions
    print("4️⃣ Getting recent sessions...")
    try:
        sessions = await get_recent_sessions(limit=5)
        print(f"✅ Found {len(sessions)} recent sessions:")
        for session in sessions:
            print(f"   - {session}")
        print()
    except Exception as e:
        print(f"❌ Failed to get sessions: {e}\n")
    
    # Test 5: Clear history
    print("5️⃣ Clearing chat history...")
    try:
        deleted = await clear_chat_history(test_session)
        print(f"✅ Deleted {deleted} messages\n")
    except Exception as e:
        print(f"❌ Failed to clear history: {e}\n")
        return
    
    # Test 6: Verify deletion
    print("6️⃣ Verifying deletion...")
    try:
        history = await get_chat_history(test_session, limit=10)
        if len(history) == 0:
            print("✅ History successfully cleared\n")
        else:
            print(f"⚠️ Warning: {len(history)} messages still exist\n")
    except Exception as e:
        print(f"❌ Failed to verify deletion: {e}\n")
    
    print("🎉 All tests completed!")


if __name__ == "__main__":
    asyncio.run(test_chat_history())
