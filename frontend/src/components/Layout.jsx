import { Outlet } from 'react-router-dom'
import Navbar from './Navbar'

export default function Layout() {
  return (
    <>
      <Navbar />
      <main style={{ flex: 1, paddingTop: 60 }}>
        <Outlet />
      </main>
    </>
  )
}
