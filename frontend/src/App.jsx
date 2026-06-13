import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import HomePage from './pages/HomePage'
import EventDetailPage from './pages/EventDetailPage'
import CreateEventPage from './pages/CreateEventPage'
import EditEventPage from './pages/EditEventPage'
import MyEventsPage from './pages/MyEventsPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'

const router = createBrowserRouter(
  [
    {
      path: '/',
      element: <Layout />,
      children: [
        { index: true, element: <HomePage /> },
        { path: 'events/:id', element: <EventDetailPage /> },
        {
          path: 'events/:id/edit',
          element: (
            <ProtectedRoute>
              <EditEventPage />
            </ProtectedRoute>
          ),
        },
        {
          path: 'events/create',
          element: (
            <ProtectedRoute>
              <CreateEventPage />
            </ProtectedRoute>
          ),
        },
        {
          path: 'my-events',
          element: (
            <ProtectedRoute>
              <MyEventsPage />
            </ProtectedRoute>
          ),
        },
      ],
    },
    { path: '/login', element: <LoginPage /> },
    { path: '/register', element: <RegisterPage /> },
    { path: '*', element: <Navigate to="/" replace /> },
  ],
  {
    future: {
      v7_startTransition: true,
      v7_normalizeFormMethod: true,
    },
  }
)

export default function App() {
  return <RouterProvider router={router} />
}
