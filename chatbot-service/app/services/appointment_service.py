"""
Appointment Service for Chatbot Integration
Handles appointment-related queries and data retrieval
"""

import logging
import asyncio
import aiohttp
import os
from typing import List, Dict, Any, Optional
from datetime import datetime, date

logger = logging.getLogger(__name__)


class AppointmentService:
    """Service for handling appointment-related chatbot queries"""
    
    def __init__(self):
        self.base_url = os.getenv("SPRING_BOOT_BASE_URL", "http://localhost:8080/api/v1")
        self.chatbot_endpoint = f"{self.base_url}/chatbot"
        self.timeout = aiohttp.ClientTimeout(total=10)
    
    async def get_appointments_for_customer(
        self,
        customer_id: Optional[int] = None,
        customer_email: Optional[str] = None,
        appointment_type: str = "all",
        auth_token: Optional[str] = None
    ) -> List[Dict[str, Any]]:
        """
        Get appointments for a customer
        
        Args:
            customer_id: Customer ID
            customer_email: Customer email
            appointment_type: Type of appointments ('all', 'available', 'upcoming')
            auth_token: JWT authentication token
        
        Returns:
            List of appointment dictionaries
        """
        try:
            params = {}
            if customer_id:
                params["customerId"] = customer_id
            if customer_email:
                params["customerEmail"] = customer_email
            if appointment_type != "all":
                params["type"] = appointment_type
            
            headers = {}
            if auth_token:
                headers["Authorization"] = f"Bearer {auth_token}"
            
            async with aiohttp.ClientSession(timeout=self.timeout) as session:
                async with session.get(
                    f"{self.chatbot_endpoint}/appointments",
                    params=params,
                    headers=headers
                ) as response:
                    if response.status == 200:
                        data = await response.json()
                        return data.get("data", [])
                    else:
                        logger.error(f"Failed to get appointments: {response.status}")
                        return []
                        
        except Exception as e:
            logger.error(f"Error getting appointments: {e}")
            return []
    
    async def get_appointment_details(
        self,
        appointment_id: int,
        auth_token: Optional[str] = None
    ) -> Optional[Dict[str, Any]]:
        """
        Get detailed information about a specific appointment
        
        Args:
            appointment_id: Appointment ID
            auth_token: JWT authentication token
        
        Returns:
            Appointment details dictionary or None
        """
        try:
            headers = {}
            if auth_token:
                headers["Authorization"] = f"Bearer {auth_token}"
                
            async with aiohttp.ClientSession(timeout=self.timeout) as session:
                async with session.get(
                    f"{self.chatbot_endpoint}/appointments/{appointment_id}",
                    headers=headers
                ) as response:
                    if response.status == 200:
                        data = await response.json()
                        return data.get("data", {})
                    else:
                        logger.error(f"Failed to get appointment {appointment_id}: {response.status}")
                        return None
                        
        except Exception as e:
            logger.error(f"Error getting appointment {appointment_id}: {e}")
            return None
    
    def format_appointments_for_chat(self, appointments: List[Dict[str, Any]]) -> str:
        """
        Format appointments data for chatbot response
        
        Args:
            appointments: List of appointment dictionaries
        
        Returns:
            Formatted string for chat response
        """
        if not appointments:
            return "I couldn't find any appointments for you."
        
        response_parts = [f"I found {len(appointments)} appointment(s) for you:"]
        
        for i, apt in enumerate(appointments, 1):
            appointment_date = apt.get("appointmentDate", "Unknown date")
            start_time = apt.get("startTime", "")
            end_time = apt.get("endTime", "")
            status = apt.get("status", "Unknown")
            vehicle_name = apt.get("vehicleName", "Unknown vehicle")
            customer_issue = apt.get("customerIssue", "")
            
            time_info = ""
            if start_time and end_time:
                time_info = f" from {start_time} to {end_time}"
            elif start_time:
                time_info = f" at {start_time}"
            
            issue_info = ""
            if customer_issue:
                issue_info = f"\n   Issue: {customer_issue}"
            
            response_parts.append(
                f"\n{i}. **{appointment_date}**{time_info}\n"
                f"   Vehicle: {vehicle_name}\n"
                f"   Status: {status.title()}{issue_info}"
            )
        
        return "\n".join(response_parts)
    
    def format_appointment_details(self, appointment: Dict[str, Any]) -> str:
        """
        Format single appointment details for chat response
        
        Args:
            appointment: Appointment dictionary
        
        Returns:
            Formatted string for chat response
        """
        if not appointment:
            return "I couldn't find the details for that appointment."
        
        appointment_date = appointment.get("appointmentDate", "Unknown date")
        start_time = appointment.get("startTime", "")
        end_time = appointment.get("endTime", "")
        status = appointment.get("status", "Unknown")
        vehicle_name = appointment.get("vehicleName", "Unknown vehicle")
        customer_issue = appointment.get("customerIssue", "")
        notes = appointment.get("notes", "")
        consultation_type = appointment.get("consultationTypeLabel", "")
        
        time_info = ""
        if start_time and end_time:
            time_info = f" from {start_time} to {end_time}"
        elif start_time:
            time_info = f" at {start_time}"
        
        response_parts = [
            f"**Appointment Details:**",
            f"📅 Date: {appointment_date}{time_info}",
            f"🚗 Vehicle: {vehicle_name}",
            f"📋 Status: {status.title()}"
        ]
        
        if consultation_type:
            response_parts.append(f"🔧 Service Type: {consultation_type}")
        
        if customer_issue:
            response_parts.append(f"❗ Issue: {customer_issue}")
        
        if notes:
            response_parts.append(f"📝 Notes: {notes}")
        
        return "\n".join(response_parts)
    
    async def process_appointment_query(
        self,
        query: str,
        customer_id: Optional[int] = None,
        customer_email: Optional[str] = None,
        auth_token: Optional[str] = None
    ) -> str:
        """
        Process appointment-related queries
        
        Args:
            query: User query about appointments
            customer_id: Customer ID
            customer_email: Customer email
            auth_token: JWT authentication token
        
        Returns:
            Formatted response string
        """
        query_lower = query.lower()
        
        # Determine query type
        if any(keyword in query_lower for keyword in ["available", "pending", "open"]):
            appointment_type = "available"
        elif any(keyword in query_lower for keyword in ["upcoming", "future", "next", "scheduled"]):
            appointment_type = "upcoming"
        else:
            appointment_type = "all"
        
        # Get appointments
        appointments = await self.get_appointments_for_customer(
            customer_id=customer_id,
            customer_email=customer_email,
            appointment_type=appointment_type,
            auth_token=auth_token
        )
        
        # Format response based on query type
        if appointment_type == "available":
            if not appointments:
                return ("I don't see any available appointment slots for you at the moment. "
                       "Would you like me to help you schedule a new appointment?")
            return self.format_appointments_for_chat(appointments)
        
        elif appointment_type == "upcoming":
            if not appointments:
                return ("You don't have any upcoming appointments scheduled. "
                       "Would you like me to help you book a new appointment?")
            return self.format_appointments_for_chat(appointments)
        
        else:  # all appointments
            if not appointments:
                return ("I don't see any appointments in your history. "
                       "Would you like me to help you schedule your first appointment?")
            return self.format_appointments_for_chat(appointments)


# Global instance
appointment_service = AppointmentService()