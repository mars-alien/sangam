import { useState, useRef, useEffect } from 'react'
import { useMap } from 'react-leaflet'
import { Search, X } from 'lucide-react'

const NOMINATIM = 'https://nominatim.openstreetmap.org/search'

export default function MapSearch({ onSelect }) {
  const map = useMap()
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)
  const timer = useRef(null)
  const wrapRef = useRef(null)

  useEffect(() => {
    function handleClick(e) {
      if (wrapRef.current && !wrapRef.current.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  function search(q) {
    setQuery(q)
    clearTimeout(timer.current)
    if (!q.trim()) { setResults([]); setOpen(false); return }
    timer.current = setTimeout(async () => {
      setLoading(true)
      try {
        const res = await fetch(
          `${NOMINATIM}?q=${encodeURIComponent(q)}&format=json&limit=5&addressdetails=1`,
          { headers: { 'Accept-Language': 'en' } }
        )
        const data = await res.json()
        setResults(data)
        setOpen(true)
      } catch { /* ignore */ }
      finally { setLoading(false) }
    }, 350)
  }

  function pick(item) {
    const lat = parseFloat(item.lat)
    const lng = parseFloat(item.lon)
    map.flyTo([lat, lng], 15, { duration: 0.8 })
    onSelect?.({ lat, lng, display_name: item.display_name, address: item.address })
    setQuery(item.display_name.split(',')[0])
    setOpen(false)
    setResults([])
  }

  function clear() {
    setQuery('')
    setResults([])
    setOpen(false)
  }

  return (
    <div
      ref={wrapRef}
      style={{
        position: 'absolute', top: 10, left: 10, right: 56, zIndex: 1000,
      }}
    >
      <div style={{
        display: 'flex', alignItems: 'center',
        background: 'var(--bg-primary, #fff)',
        border: '1.5px solid rgba(0,0,0,0.18)',
        borderRadius: 8,
        boxShadow: '0 2px 8px rgba(0,0,0,0.18)',
        overflow: 'hidden',
        height: 36,
      }}>
        <span style={{ padding: '0 8px 0 10px', color: '#6B7280', display: 'flex', alignItems: 'center', flexShrink: 0 }}>
          {loading
            ? <span style={{ width: 16, height: 16, border: '2px solid #6B7280', borderTopColor: 'transparent', borderRadius: '50%', display: 'inline-block', animation: 'spin 0.7s linear infinite' }} />
            : <Search size={15} />}
        </span>
        <input
          type="text"
          value={query}
          onChange={(e) => search(e.target.value)}
          onFocus={() => results.length && setOpen(true)}
          placeholder="Search place…"
          style={{
            flex: 1, border: 'none', outline: 'none', background: 'transparent',
            fontSize: '0.85rem', color: 'var(--text-primary)', padding: '0 4px',
          }}
        />
        {query && (
          <button
            type="button"
            onClick={clear}
            style={{ border: 'none', background: 'transparent', padding: '0 8px', cursor: 'pointer', color: '#6B7280', display: 'flex', alignItems: 'center' }}
          >
            <X size={14} />
          </button>
        )}
      </div>

      {open && results.length > 0 && (
        <ul style={{
          margin: 0, padding: 0, listStyle: 'none',
          background: 'var(--bg-primary, #fff)',
          border: '1.5px solid rgba(0,0,0,0.12)',
          borderRadius: 8, marginTop: 4,
          boxShadow: '0 4px 16px rgba(0,0,0,0.18)',
          maxHeight: 220, overflowY: 'auto',
        }}>
          {results.map((r) => (
            <li
              key={r.place_id}
              onClick={() => pick(r)}
              style={{
                padding: '9px 12px', cursor: 'pointer',
                borderBottom: '1px solid rgba(0,0,0,0.06)',
                fontSize: '0.82rem', lineHeight: 1.4,
              }}
              onMouseEnter={(e) => e.currentTarget.style.background = 'var(--bg-secondary, #f3f4f6)'}
              onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
            >
              <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                {r.display_name.split(',')[0]}
              </div>
              <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginTop: 1 }}>
                {r.display_name.split(',').slice(1, 3).join(',')}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
