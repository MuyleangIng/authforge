# AuthForge React SDK

## Installation

```bash
npm install @authforge/react
```

## Setup

Wrap your app with `AuthForgeProvider`:

```jsx
import { AuthForgeProvider, useAuth } from '@authforge/react'

function App() {
  return (
    <AuthForgeProvider
      url="http://localhost:8080"
      realm="myrealm"
      clientId="my-react-app"
    >
      <Router />
    </AuthForgeProvider>
  )
}
```

## useAuth Hook

```jsx
function Profile() {
  const { user, token, login, logout, hasRole, isAuthenticated } = useAuth()

  if (!isAuthenticated) {
    return <button onClick={login}>Login</button>
  }

  return (
    <div>
      <p>Welcome, {user.preferred_username}</p>
      {hasRole('admin') && <AdminPanel />}
      <button onClick={logout}>Logout</button>
    </div>
  )
}
```

## API Reference

| Method / Property | Description |
|---|---|
| `isAuthenticated` | Boolean — true if user is logged in |
| `user` | OIDC user object (sub, email, roles, etc.) |
| `token` | Current access token string |
| `login()` | Redirect to AuthForge login page |
| `logout()` | Clear session and redirect |
| `hasRole(role)` | Check if user has a specific role |
| `getRoles()` | Returns array of role names |
