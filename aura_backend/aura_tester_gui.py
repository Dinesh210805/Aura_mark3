import streamlit as st
import requests

st.title("AURA Backend Tester")

backend_url = st.text_input("Backend URL", "http://localhost:8000")

st.header("Test /process Endpoint (Voice Command)")
audio_file = st.file_uploader("Upload audio file (.wav, .mp3)", type=["wav", "mp3"])
screenshot_file = st.file_uploader("Upload screenshot (optional, .png)", type=["png"])
ui_tree = st.text_area("UI Tree (optional)")
session_id = st.text_input("Session ID (optional)")

if st.button("Send to /process"):
    if not audio_file:
        st.error("Please upload an audio file.")
    else:
        files = {
            "audio": (audio_file.name, audio_file, audio_file.type),
        }
        if screenshot_file:
            files["screenshot"] = (screenshot_file.name, screenshot_file, screenshot_file.type)
        data = {}
        if ui_tree:
            data["ui_tree"] = ui_tree
        if session_id:
            data["session_id"] = session_id
        try:
            response = requests.post(f"{backend_url}/process", files=files, data=data)
            st.subheader("Response")
            st.json(response.json())
        except Exception as e:
            st.error(f"Request failed: {e}")

st.header("Test /chat Endpoint (Text Only)")
chat_text = st.text_input("Chat Text")
chat_session_id = st.text_input("Chat Session ID (optional)", key="chat_session_id")

if st.button("Send to /chat"):
    if not chat_text:
        st.error("Please enter chat text.")
    else:
        payload = {"text": chat_text}
        if chat_session_id:
            payload["session_id"] = chat_session_id
        try:
            response = requests.post(f"{backend_url}/chat", json=payload)
            st.subheader("Response")
            st.json(response.json())
        except Exception as e:
            st.error(f"Request failed: {e}") 