import { Module } from '@nestjs/common'
import { ConfigService } from './config.service'
import { ConfigController } from './config.controller'
import { PrismaService } from 'src/prisma.service'

@Module({
  providers: [ConfigService, PrismaService],
  controllers: [ConfigController],
})
export class ConfigModule {}
