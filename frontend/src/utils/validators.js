import { z } from 'zod'

export const loginSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(1, 'Password is required'),
})

export const registerSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z
    .string()
    .min(8, 'At least 8 characters')
    .regex(/[A-Z]/, 'Must include an uppercase letter')
    .regex(/[0-9!@#$%^&*]/, 'Must include a number or special character'),
  username: z
    .string()
    .min(3, 'At least 3 characters')
    .max(30, 'Max 30 characters')
    .regex(/^[a-zA-Z0-9_]+$/, 'Letters, numbers and underscores only'),
  displayName: z.string().min(1, 'Display name is required').max(60),
})

export const createEventSchema = z
  .object({
    title: z.string().min(3, 'At least 3 characters').max(120),
    description: z.string().min(10, 'At least 10 characters').max(2000),
    category: z.string().min(1, 'Select a category'),
    venueName: z.string().min(1, 'Venue name is required'),
    address: z.string().min(1, 'Address is required'),
    city: z.string().min(1, 'City is required'),
    eventDate: z.string().min(1, 'Date is required'),
    eventTime: z.string().min(1, 'Time is required'),
    minCompanions: z.coerce.number().int().min(1).default(1),
    maxCompanions: z.coerce.number().int().min(1, 'At least 1 companion').max(20),
    tags: z.string().optional(),
  })
  .refine((d) => d.maxCompanions >= d.minCompanions, {
    message: 'Max companions must be ≥ min companions',
    path: ['maxCompanions'],
  })
