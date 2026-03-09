# AuthForge Python SDK

## Installation

```bash
pip install authforge-python
```

## Setup

```python
from authforge import AuthForgeClient

client = AuthForgeClient(
    url="http://localhost:8080",
    realm="myrealm",
    client_id="my-python-api",
    client_secret="your-secret"
)
```

## FastAPI Integration

```python
from fastapi import FastAPI, Depends
from authforge.fastapi import require_auth, require_role, get_current_user

app = FastAPI()

# Require any authenticated user
@app.get("/me")
@require_auth(client)
async def me(user=Depends(get_current_user(client))):
    return {
        "sub": user.sub,
        "email": user.email,
        "roles": user.roles
    }

# Require specific role
@app.delete("/users/{user_id}")
@require_role(client, "admin")
async def delete_user(user_id: str, user=Depends(get_current_user(client))):
    # Only admins can reach here
    return {"deleted": user_id}
```

## Direct Token Validation

```python
# Validate an access token
token_info = client.introspect("eyJhbGciOiJIUzI1NiJ9...")
if token_info["active"]:
    print("Token valid for user:", token_info["sub"])

# Get a token for M2M (client credentials)
m2m_token = client.get_client_credentials_token()
```

## Django Integration

```python
# settings.py
INSTALLED_APPS = [
    ...
    'authforge.django',
]

AUTHFORGE = {
    'URL': 'http://localhost:8080',
    'REALM': 'myrealm',
    'CLIENT_ID': 'my-django-app',
    'CLIENT_SECRET': 'your-secret',
}

# views.py
from authforge.django import AuthForgeLoginRequired, require_role

@AuthForgeLoginRequired
def profile(request):
    return JsonResponse({"user": request.authforge_user})

@require_role("admin")
def admin_dashboard(request):
    return render(request, "admin/dashboard.html")
```
