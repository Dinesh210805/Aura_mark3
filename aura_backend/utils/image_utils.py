from PIL import Image
import io
import logging

logger = logging.getLogger(__name__)

def validate_image(image_data: bytes) -> bool:
    """Validate that the image data is a valid image"""
    try:
        with Image.open(io.BytesIO(image_data)) as img:
            img.verify()
        return True
    except Exception as e:
        logger.error(f"Image validation failed: {str(e)}")
        return False

def optimize_image(image_data: bytes, max_size: tuple = (1920, 1080), quality: int = 85) -> bytes:
    """Optimize image for VLM processing"""
    try:
        with Image.open(io.BytesIO(image_data)) as img:
            # Convert to RGB if necessary
            if img.mode != 'RGB':
                img = img.convert('RGB')
            
            # Resize if too large (VLM processing works better with smaller images)
            if img.size[0] > max_size[0] or img.size[1] > max_size[1]:
                logger.info(f"Resizing image from {img.size} to fit {max_size}")
                img.thumbnail(max_size, Image.Resampling.LANCZOS)
            
            # Save optimized image
            output = io.BytesIO()
            img.save(output, format='JPEG', quality=quality, optimize=True)
            optimized_data = output.getvalue()
            
            logger.info(f"Image optimized: {len(image_data)} -> {len(optimized_data)} bytes")
            return optimized_data
            
    except Exception as e:
        logger.error(f"Image optimization failed: {str(e)}")
        return image_data  # Return original if optimization fails

def get_image_info(image_data: bytes) -> dict:
    """Get basic information about an image"""
    try:
        with Image.open(io.BytesIO(image_data)) as img:
            return {
                "format": img.format,
                "mode": img.mode,
                "size": img.size,
                "bytes": len(image_data)
            }
    except Exception as e:
        logger.error(f"Failed to get image info: {str(e)}")
        return {"error": str(e)}

def validate_audio(audio_data: bytes) -> bool:
    """Basic validation for audio data"""
    try:
        # Basic checks for audio data
        if len(audio_data) < 1000:  # Too small to be valid audio
            return False
        
        # Check for WAV header
        if audio_data[:4] == b'RIFF' and audio_data[8:12] == b'WAVE':
            return True
        
        # Check for other common audio formats (MP3, etc.)
        if audio_data[:3] == b'ID3' or audio_data[:2] == b'\xff\xfb':
            return True
        
        # If we can't identify the format, assume it's valid
        return True
        
    except Exception as e:
        logger.error(f"Audio validation failed: {str(e)}")
        return False

def estimate_audio_duration(audio_data: bytes) -> float:
    """Estimate audio duration in seconds (rough calculation)"""
    try:
        # This is a very rough estimation
        # For WAV files, we could parse the header for accurate duration
        # For now, use file size as approximation (assuming 16kHz, 16-bit mono)
        estimated_seconds = len(audio_data) / (16000 * 2)  # 16kHz * 2 bytes per sample
        return max(0.1, min(60.0, estimated_seconds))  # Clamp between 0.1 and 60 seconds
    except Exception:
        return 1.0  # Default estimate
