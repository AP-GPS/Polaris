import { Injectable, NotFoundException } from '@nestjs/common'
import { PrismaService } from 'src/prisma.service'
import { UpdateConfigDto } from './dto/update-config.dto'

@Injectable()
export class ConfigService {
  constructor(private readonly prisma: PrismaService) {}

  async fetch() {
    const cfg = await this.prisma.config.findUnique({ where: { id: 1 } })
    if (!cfg) throw new NotFoundException('Config not found')
    return {
      intervalMinutes: cfg.intervalMinutes,
      thresholdSignal: cfg.thresholdSignal,
      thresholdDownload: cfg.thresholdDownload,
      thresholdUpload: cfg.thresholdUpload,
      thresholdPing: cfg.thresholdPing,
    }
  }

 async update(dto: UpdateConfigDto) {
   return this.prisma.config.upsert({
     where: { id: 1 },
     update: dto,
     create: { id: 1, ...dto },
   })
 }
}
