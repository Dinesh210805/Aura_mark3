"""
Screenshot utilities for AURA test GUI
Handles sample screenshot generation and validation
"""

from PIL import Image, ImageDraw, ImageFont
import io
import base64

def create_sample_screenshot(screen_type: str, width: int = 400, height: int = 800) -> Image.Image:
    """
    Create a sample Android screenshot for testing.
    
    Args:
        screen_type: Type of screen to simulate
        width: Screenshot width
        height: Screenshot height
    
    Returns:
        PIL Image object
    """
    # Create base image
    img = Image.new('RGB', (width, height), color='#f0f0f0')
    draw = ImageDraw.Draw(img)
    
    # Try to use a simple font
    try:
        font = ImageFont.truetype("arial.ttf", 20)
        small_font = ImageFont.truetype("arial.ttf", 16)
    except:
        font = ImageFont.load_default()
        small_font = font
    
    if screen_type == "Android Home Screen":
        # Status bar
        draw.rectangle([0, 0, width, 50], fill='#2196f3')
        draw.text((10, 15), "12:34", fill='white', font=font)
        draw.text((width-60, 15), "100%", fill='white', font=small_font)
        
        # App icons grid
        icon_size = 60
        margin = 20
        cols = 4
        rows = 6
        
        app_names = ["WhatsApp", "Gmail", "Settings", "Camera", 
                    "Chrome", "Maps", "YouTube", "Photos",
                    "Contacts", "Phone", "Messages", "Calendar",
                    "Gallery", "Music", "Files", "Weather",
                    "Notes", "Clock", "Calculator", "Store",
                    "Facebook", "Instagram", "Twitter", "Spotify"]
        
        for row in range(rows):
            for col in range(cols):
                if row * cols + col < len(app_names):
                    x = margin + col * (icon_size + margin)
                    y = 100 + row * (icon_size + 40)
                    
                    # App icon
                    draw.rectangle([x, y, x + icon_size, y + icon_size], 
                                 fill='#4caf50', outline='#333333')
                    
                    # App name
                    app_name = app_names[row * cols + col]
                    text_width = draw.textlength(app_name, font=small_font)
                    draw.text((x + (icon_size - text_width) // 2, y + icon_size + 5), 
                             app_name, fill='#333333', font=small_font)
        
        # Bottom navigation
        draw.rectangle([0, height-80, width, height], fill='#ffffff')
        draw.line([0, height-80, width, height-80], fill='#cccccc')
        
        # Navigation buttons
        nav_buttons = ["⬅", "⭕", "⬜"]
        for i, btn in enumerate(nav_buttons):
            x = (width // 3) * i + (width // 6)
            draw.text((x-10, height-50), btn, fill='#333333', font=font)
    
    elif screen_type == "WhatsApp Chat":
        # WhatsApp header
        draw.rectangle([0, 0, width, 80], fill='#075e54')
        draw.text((50, 25), "John Doe", fill='white', font=font)
        draw.text((10, 25), "⬅", fill='white', font=font)
        draw.text((width-40, 25), "⋮", fill='white', font=font)
        
        # Chat messages
        messages = [
            {"text": "Hey, how are you?", "sender": "them", "y": 120},
            {"text": "I'm good! How about you?", "sender": "me", "y": 180},
            {"text": "Great! Want to meet up later?", "sender": "them", "y": 240},
            {"text": "Sure, what time works?", "sender": "me", "y": 300},
        ]
        
        for msg in messages:
            if msg["sender"] == "me":
                # My message (right side, green)
                msg_width = min(250, draw.textlength(msg["text"], font=small_font) + 20)
                x = width - msg_width - 20
                draw.rectangle([x, msg["y"], x + msg_width, msg["y"] + 40], 
                             fill='#dcf8c6', outline='#cccccc')
            else:
                # Their message (left side, white)
                msg_width = min(250, draw.textlength(msg["text"], font=small_font) + 20)
                x = 20
                draw.rectangle([x, msg["y"], x + msg_width, msg["y"] + 40], 
                             fill='#ffffff', outline='#cccccc')
            
            # Message text
            draw.text((x + 10, msg["y"] + 10), msg["text"], fill='#333333', font=small_font)
        
        # Input area
        draw.rectangle([0, height-80, width, height], fill='#f0f0f0')
        draw.rectangle([20, height-60, width-100, height-20], fill='white', outline='#cccccc')
        draw.text((30, height-45), "Type a message...", fill='#999999', font=small_font)
        
        # Send button
        draw.circle((width-50, height-40), 20, fill='#25d366')
        draw.text((width-55, height-48), "➤", fill='white', font=small_font)
    
    elif screen_type == "Settings Menu":
        # Settings header
        draw.rectangle([0, 0, width, 80], fill='#2196f3')
        draw.text((50, 25), "Settings", fill='white', font=font)
        draw.text((10, 25), "⬅", fill='white', font=font)
        
        # Settings options
        settings = [
            "📶 Network & Internet",
            "🔗 Connected devices",
            "🖥️ Display",
            "🔊 Sound",
            "📱 Apps",
            "🔋 Battery",
            "💾 Storage",
            "🔒 Privacy",
            "🛡️ Security",
            "🌐 System"
        ]
        
        for i, setting in enumerate(settings):
            y = 100 + i * 60
            # Setting item background
            if i % 2 == 0:
                draw.rectangle([0, y, width, y + 60], fill='#fafafa')
            
            # Setting text
            draw.text((20, y + 20), setting, fill='#333333', font=small_font)
            
            # Arrow
            draw.text((width-30, y + 20), "›", fill='#666666', font=font)
    
    elif screen_type == "Gmail Compose":
        # Gmail header
        draw.rectangle([0, 0, width, 80], fill='#ea4335')
        draw.text((50, 25), "Compose", fill='white', font=font)
        draw.text((10, 25), "⬅", fill='white', font=font)
        draw.text((width-60, 25), "Send", fill='white', font=small_font)
        
        # Compose fields
        fields = [
            {"label": "To:", "y": 100},
            {"label": "Subject:", "y": 160},
        ]
        
        for field in fields:
            draw.text((20, field["y"]), field["label"], fill='#333333', font=small_font)
            draw.rectangle([20, field["y"] + 25, width-20, field["y"] + 55], 
                         fill='white', outline='#cccccc')
        
        # Message body
        draw.text((20, 240), "Message:", fill='#333333', font=small_font)
        draw.rectangle([20, 265, width-20, 500], fill='white', outline='#cccccc')
        draw.text((30, 280), "Type your message here...", fill='#999999', font=small_font)
        
        # Toolbar
        draw.rectangle([0, height-60, width, height], fill='#f5f5f5')
        toolbar_items = ["📎", "🖼️", "😊", "⋮"]
        for i, item in enumerate(toolbar_items):
            x = 30 + i * 80
            draw.text((x, height-40), item, fill='#666666', font=font)
    
    return img

def image_to_base64(image: Image.Image) -> str:
    """Convert PIL Image to base64 string."""
    buffer = io.BytesIO()
    image.save(buffer, format="PNG")
    img_str = base64.b64encode(buffer.getvalue()).decode()
    return img_str

def create_sample_screenshot_file(screen_type: str) -> io.BytesIO:
    """Create a sample screenshot and return as file-like object."""
    img = create_sample_screenshot(screen_type)
    img_bytes = io.BytesIO()
    img.save(img_bytes, format="PNG")
    img_bytes.seek(0)
    return img_bytes

# Available sample screenshots
SAMPLE_SCREENSHOTS = {
    "Android Home Screen": "Main Android launcher with app icons",
    "WhatsApp Chat": "WhatsApp conversation interface",
    "Settings Menu": "Android system settings",
    "Gmail Compose": "Gmail email composition screen"
}
