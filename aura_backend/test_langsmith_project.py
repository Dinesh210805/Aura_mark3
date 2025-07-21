"""
Direct LangSmith project test
"""

import os
from langsmith import Client
from dotenv import load_dotenv

load_dotenv()

def test_langsmith_project():
    """Test if the LangSmith project exists and can be accessed"""
    
    api_key = os.getenv("LANGCHAIN_API_KEY")
    project_name = os.getenv("LANGCHAIN_PROJECT", "aura-agent-visualization")
    
    if not api_key:
        print("❌ No LangSmith API key found")
        return False
    
    try:
        client = Client(api_key=api_key)
        print(f"✅ LangSmith client initialized")
        
        # Try to list projects to see if we can access the API
        try:
            projects = list(client.list_projects())
            print(f"✅ Found {len(projects)} projects")
            
            # Check if our project exists
            project_exists = False
            for project in projects:
                if project.name == project_name:
                    project_exists = True
                    print(f"✅ Project '{project_name}' exists with ID: {project.id}")
                    break
            
            if not project_exists:
                print(f"⚠️  Project '{project_name}' not found. Creating it...")
                # Try to create project by making a test trace
                try:
                    from langsmith.run_helpers import traceable
                    
                    @traceable(project_name=project_name)
                    def test_function():
                        return {"status": "project_creation_test"}
                    
                    result = test_function()
                    print(f"✅ Test trace created: {result}")
                    return True
                    
                except Exception as e:
                    print(f"❌ Failed to create project: {e}")
                    return False
            
            return True
            
        except Exception as e:
            print(f"❌ Failed to list projects: {e}")
            return False
            
    except Exception as e:
        print(f"❌ Failed to initialize client: {e}")
        return False

if __name__ == "__main__":
    success = test_langsmith_project()
    if success:
        print(f"\n🎯 Go to: https://smith.langchain.com/projects/p/aura-agent-visualization")
    else:
        print("\n❌ LangSmith project setup failed")
