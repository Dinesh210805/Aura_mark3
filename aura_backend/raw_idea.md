    my thoughts:

    hi there, copilot, we are going to develop a new project that will be very **useful** to people with visual impairments, motor impairments, even normal mob who are busy with their hands. The project name is **AURA: Autonomous UI Reader and Assistant**, so we are going to develop this together.

    **About the idea:**

    Here we are going to control the whole mobile with voice, so the main doubt is how are we gonna control the phone with voice? So the major doubts would be like **how?**

    So I will give you my answer. That is, we are going to build an app only for Android — where we will interact with the Agent AURA with our voice command that will be processed by an **STT (speech-to-text model)** → which converts the voice into text. Then we must understand the user's intent, right!? So we will use **LLM (large language models)** to understand the intent of the command. Now comes the major part: **how** we will be doing the task? So here we go. Remember the project name “AURA”? Here you can see **Autonomous UI Reader** — this name shows what we are gonna do. Now we will use two approaches here: that is, **Android Accessibility Services** to locate the UI elements, and another one — big bro entry — is **VLM (vision language models)**. They come into play. They are powerful models that can understand images. The thing is, nowadays the data inside apps and the mobile are dynamic — only Android Accessibility Services won't be enough. So we are using the VLM in addition too. Now we understood the UI, and we must create step-by-step procedures to accomplish the user's goal mentioned in the voice. So since VLMs are multimodal (they can process both images and text), we will use them to locate the UI elements — like give us the coordinates (based on the **resolution** of the image, we can get the coordinates, right?) — and also we can use Android Accessibility Services and create step-by-step procedures and simulate the touch or swipe using Android Accessibility Services. And if the page changes, again use the Android **Accessibility Service** and VLM to understand and act, and use **TTS (text-to-speech)** to inform the users about the process. And for typing in fields, we can use LLMs and Android Accessibility Services.

    ---

    So now tech stack — I'm not **confirmed**, but here is what I have in my mind:

    For **GenAI stack**, we will be using models from **Groq Cloud**

    (**STT (whisper-large-v3-turbo)**,

    **LLM (llama-3.3-70b-versatile)**,

    **VLM (meta-llama/llama-4-maverick-17b-128e-instruct)**,

    **TTS (playai-tts)**)
