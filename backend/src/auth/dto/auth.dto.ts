import { ApiProperty } from '@nestjs/swagger'
import { IsString } from 'class-validator'

export class AuthDto {
  @ApiProperty({ description: 'نام کاربری', example: 'user1' })
  @IsString()
  username: string

  @ApiProperty({ description: 'کلمه عبور', example: 'pass1234' })
  @IsString()
  password: string
}
