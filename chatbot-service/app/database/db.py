"""Database connection and data fetching utilities"""

import os
import logging
from typing import List, Dict, Any
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.orm import sessionmaker
from sqlalchemy import text

logger = logging.getLogger(__name__)

# Database connection
DATABASE_URL = os.getenv("DATABASE_URL")
if not DATABASE_URL:
    raise ValueError("DATABASE_URL not set in environment")

# Lazy initialization of engine
_engine = None

def get_engine():
    """Get or create the database engine"""
    global _engine
    if _engine is None:
        # asyncpg requires ssl=True instead of sslmode parameter
        connect_args = {
            "ssl": "require"  # Enable SSL for Neon/cloud databases
        }
        _engine = create_async_engine(
            DATABASE_URL, 
            echo=False, 
            future=True,
            connect_args=connect_args
        )
    return _engine

async_session = None

def get_async_session():
    """Get or create the async session maker"""
    global async_session
    if async_session is None:
        async_session = sessionmaker(get_engine(), class_=AsyncSession, expire_on_commit=False)
    return async_session


async def get_db_connection():
    """Get database session"""
    session_maker = get_async_session()
    async with session_maker() as session:
        yield session


async def fetch_appointment_data() -> List[Dict[str, Any]]:
    """
    Fetch appointment data from PostgreSQL database
    Converts appointments to text format for embeddings
    
    Returns:
        List of documents with text and metadata
    """
    try:
        session_maker = get_async_session()
        async with session_maker() as session:
            # Query to fetch appointments with related data
            query = text("""
                SELECT 
                    a.appointment_id,
                    a.date,
                    a.start_time,
                    a.end_time,
                    a.status,
                    a.notes,
                    u.name as customer_name,
                    u.email as customer_email,
                    v.make,
                    v.model,
                    v.year,
                    e_user.name as mechanic_name
                FROM appointment a
                LEFT JOIN customers c ON a.customer_id = c.customer_id
                LEFT JOIN users u ON c.user_id = u.user_id
                LEFT JOIN vehicle v ON a.vehicle_id = v.vehicle_id
                LEFT JOIN employees e ON a.mechanic_id = e.employee_id
                LEFT JOIN users e_user ON e.user_id = e_user.user_id
                WHERE a.status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS')
                ORDER BY a.date ASC, a.start_time ASC
            """)
            
            result = await session.execute(query)
            appointments = result.fetchall()
            
            documents = []
            for apt in appointments:
                # Convert appointment to natural language text
                apt_text = f"""
Appointment ID: {apt.appointment_id}
Date: {apt.date}
Time: {apt.start_time} - {apt.end_time}
Status: {apt.status}
Customer: {apt.customer_name} ({apt.customer_email})
Vehicle: {apt.year} {apt.make} {apt.model}
Mechanic: {apt.mechanic_name or 'Not assigned'}
Notes: {apt.notes or 'None'}
"""
                
                documents.append({
                    "id": f"appointment_{apt.appointment_id}",
                    "text": apt_text.strip(),
                    "metadata": {
                        "appointment_id": apt.appointment_id,
                        "date": str(apt.date),
                        "status": apt.status,
                        "customer_name": apt.customer_name,
                        "vehicle": f"{apt.year} {apt.make} {apt.model}",
                        "source": f"Appointment #{apt.appointment_id}",
                        "text": apt_text.strip()
                    }
                })
            
            return documents
    
    except Exception as e:
        logger.error(f"Error fetching appointment data: {e}", exc_info=True)
        return []


async def fetch_available_slots(date: str = None) -> List[Dict[str, Any]]:
    """
    Fetch available time slots for appointments
    
    Args:
        date: Optional date filter (YYYY-MM-DD)
    
    Returns:
        List of available slots
    """
    try:
        session_maker = get_async_session()
        async with session_maker() as session:
            # Define working hours (9 AM - 6 PM)
            # Query existing appointments to find gaps
            
            if date:
                query = text("""
                    SELECT 
                        date,
                        start_time,
                        end_time
                    FROM appointment
                    WHERE date = :date
                    AND status IN ('CONFIRMED', 'IN_PROGRESS')
                    ORDER BY start_time
                """)
                result = await session.execute(query, {"date": date})
            else:
                query = text("""
                    SELECT 
                        date,
                        start_time,
                        end_time
                    FROM appointment
                    WHERE date >= CURRENT_DATE
                    AND status IN ('CONFIRMED', 'IN_PROGRESS')
                    ORDER BY date, start_time
                """)
                result = await session.execute(query)
            
            booked_slots = result.fetchall()
            
            # Calculate available slots (simplified - you may need more complex logic)
            # This is a placeholder - implement your slot calculation logic
            
            return [
                {
                    "date": str(slot.date),
                    "start_time": str(slot.start_time),
                    "end_time": str(slot.end_time)
                }
                for slot in booked_slots
            ]
    
    except Exception as e:
        logger.error(f"Error fetching available slots: {e}", exc_info=True)
        return []
