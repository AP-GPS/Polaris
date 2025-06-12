import { Injectable, UnauthorizedException, BadRequestException } from '@nestjs/common'
import { PrismaService } from 'src/prisma.service'
import * as bcrypt from 'bcryptjs'
import { JwtService } from '@nestjs/jwt'

@Injectable()
export class AuthService {
  constructor(
    private prisma: PrismaService,
    private jwt: JwtService,
  ) {}

  async register(username: string, password: string) {
    const hash = await bcrypt.hash(password, 12)
    try {
      const user = await this.prisma.user.create({
        data: { username, password: hash, role: 'USER' },
      })
      const token = this.jwt.sign({ sub: user.id, role: user.role })
      return { token }
    } catch (err) {
      if (err.code === 'P2002') { // unique constraint
        throw new BadRequestException('Username already exists')
      }
      throw err
    }
  }

  async login(username: string, password: string) {
    const user = await this.prisma.user.findUnique({ where: { username } })
    if (!user) throw new UnauthorizedException('Invalid credentials')
    const valid = await bcrypt.compare(password, user.password)
    if (!valid) throw new UnauthorizedException('Invalid credentials')
    const token = this.jwt.sign({ sub: user.id, role: user.role })
    return { token }
  }
}
