import { forwardRef } from 'react'

const Input = forwardRef(function Input(
  { label, error, className = '', as: Tag = 'input', ...props },
  ref
) {
  return (
    <div className="input-wrapper">
      {label && <label className="input-label">{label}</label>}
      <Tag
        ref={ref}
        className={`input-field ${error ? 'error' : ''} ${className}`}
        {...props}
      />
      {error && <span className="input-error">{error}</span>}
    </div>
  )
})

export default Input
