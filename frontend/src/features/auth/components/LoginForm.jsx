import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import Input from '../../../components/Input'
import Button from '../../../components/Button'
import { loginSchema } from '../../../utils/validators'

export default function LoginForm({ onSubmit, isLoading, apiError }) {
  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(loginSchema),
  })

  return (
    <form className="auth-form" onSubmit={handleSubmit(onSubmit)}>
      {apiError && <div className="api-error">{apiError}</div>}
      <Input
        label="Email"
        type="email"
        placeholder="you@example.com"
        autoComplete="email"
        error={errors.email?.message}
        {...register('email')}
      />
      <Input
        label="Password"
        type="password"
        placeholder="••••••••"
        autoComplete="current-password"
        error={errors.password?.message}
        {...register('password')}
      />
      <Button type="submit" variant="primary" full loading={isLoading}>
        {isLoading ? 'Logging in…' : 'Log In'}
      </Button>
    </form>
  )
}
