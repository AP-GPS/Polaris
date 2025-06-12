import { Controller, Post, Body, HttpCode } from '@nestjs/common'
import { AuthService } from './auth.service'
import { AuthDto } from './dto/auth.dto'
import { ApiTags, ApiOperation, ApiResponse, ApiBody } from '@nestjs/swagger'

@ApiTags('Authentication')
@Controller('auth')
export class AuthController {
  constructor(private auth: AuthService) {}

  @Post('register')
  @ApiOperation({ summary: 'Sign up a new user' })
  @ApiBody({ type: AuthDto })
  @ApiResponse({ status: 201, description: 'User registered successfully' })
  @ApiResponse({ status: 400, description: 'Invalid request data' })
  async register(@Body() dto: AuthDto) {
    return this.auth.register(dto.username, dto.password)
  }

  @HttpCode(200)
  @Post('login')
  @ApiOperation({ summary: 'Log in a user' })
  @ApiBody({ type: AuthDto })
  @ApiResponse({ status: 200, description: 'Login successful, returns JWT token' })
  @ApiResponse({ status: 401, description: 'Invalid username or password' })
  async login(@Body() dto: AuthDto) {
    return this.auth.login(dto.username, dto.password)
  }
}
