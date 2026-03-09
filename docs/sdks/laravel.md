# AuthForge Laravel SDK

## Installation

```bash
composer require authforge/laravel
```

## Configuration

Publish the config file:

```bash
php artisan vendor:publish --provider="AuthForge\Laravel\AuthForgeServiceProvider"
```

Edit `config/authforge.php`:

```php
return [
    'url'           => env('AUTHFORGE_URL', 'http://localhost:8080'),
    'realm'         => env('AUTHFORGE_REALM', 'myrealm'),
    'client_id'     => env('AUTHFORGE_CLIENT_ID', 'my-laravel-app'),
    'client_secret' => env('AUTHFORGE_CLIENT_SECRET', ''),
];
```

## Route Protection

```php
use Illuminate\Support\Facades\Route;

// Require any authenticated user
Route::middleware('authforge')->group(function () {
    Route::get('/profile', [ProfileController::class, 'show']);
    Route::get('/dashboard', [DashboardController::class, 'index']);
});

// Require specific role
Route::middleware('authforge:admin')->group(function () {
    Route::apiResource('users', AdminUserController::class);
    Route::apiResource('realms', AdminRealmController::class);
});
```

## Accessing the Authenticated User

```php
// In a controller
use Illuminate\Http\Request;

class ProfileController extends Controller
{
    public function show(Request $request)
    {
        $user = auth()->user(); // AuthForge UserPrincipal object

        return response()->json([
            'id'       => $user->getId(),
            'email'    => $user->getEmail(),
            'username' => $user->getUsername(),
            'roles'    => $user->getRoles(),
        ]);
    }

    public function adminAction(Request $request)
    {
        $user = auth()->user();

        if (!$user->hasRole('admin')) {
            abort(403, 'Admin role required');
        }

        // admin-only logic
    }
}
```

## Environment Variables

```env
AUTHFORGE_URL=http://localhost:8080
AUTHFORGE_REALM=myrealm
AUTHFORGE_CLIENT_ID=my-laravel-app
AUTHFORGE_CLIENT_SECRET=your-client-secret
```
