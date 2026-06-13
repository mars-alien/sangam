import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import Input from '../../../components/Input'
import Button from '../../../components/Button'
import { registerSchema } from '../../../utils/validators'

export default function RegisterForm({ onSubmit, isLoading, apiError }) {
  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(registerSchema),
  })

  return (
    <form className="auth-form" onSubmit={handleSubmit(onSubmit)}>
      {apiError && <div className="api-error">{apiError}</div>}
      <div className="form-row">
        <Input
          label="Username"
          placeholder="johndoe"
          autoComplete="username"
          error={errors.username?.message}
          {...register('username')}
        />
        <Input
          label="Display name"
          placeholder="John Doe"
          error={errors.displayName?.message}
          {...register('displayName')}
        />
      </div>
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
        placeholder="Min 8 chars, 1 uppercase, 1 number/symbol"
        autoComplete="new-password"
        error={errors.password?.message}
        {...register('password')}
      />
      <Button type="submit" variant="primary" full loading={isLoading}>
        {isLoading ? 'Creating account…' : 'Create Account'}
      </Button>
    </form>
  )
}
