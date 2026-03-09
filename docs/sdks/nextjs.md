# AuthForge Next.js SDK

## Installation

```bash
npm install @authforge/nextjs
```

## Middleware (Route Protection)

```typescript
// middleware.ts
import { withAuth } from '@authforge/nextjs'

export default withAuth({
  realm: 'myrealm',
  clientId: 'my-next-app',
})

export const config = {
  matcher: ['/dashboard/:path*', '/admin/:path*'],
}
```

## Server Components

```typescript
// app/dashboard/page.tsx
import { getServerSession } from '@authforge/nextjs/server'

export default async function DashboardPage() {
  const session = await getServerSession()

  if (!session) {
    redirect('/login')
  }

  return <div>Hello, {session.user.preferred_username}</div>
}
```

## Client Components

```tsx
'use client'
import { useAuth } from '@authforge/nextjs/client'

export function NavBar() {
  const { user, login, logout } = useAuth()

  return (
    <nav>
      {user ? (
        <>
          <span>{user.email}</span>
          <button onClick={logout}>Sign out</button>
        </>
      ) : (
        <button onClick={login}>Sign in</button>
      )}
    </nav>
  )
}
```

## Environment Variables

```env
AUTHFORGE_URL=http://localhost:8080
AUTHFORGE_REALM=myrealm
AUTHFORGE_CLIENT_ID=my-next-app
AUTHFORGE_CLIENT_SECRET=your-client-secret
```
