import { Injectable } from '@nestjs/common'
import { CreateMeasurementDto } from './dto/create-measurements.dto'
import { PrismaService } from 'src/prisma.service'

@Injectable()
export class MeasurementsService {
  constructor(private prisma: PrismaService) {}

  async create(userId: number, dto: CreateMeasurementDto) {
    const measurement = await this.prisma.measurement.create({
      data: { userId, ...dto },
    })
    return {
      ...measurement,
      timestamp: Number(measurement.timestamp),
    }
  }

  async findAll(userId: number) {
    const list = await this.prisma.measurement.findMany({
      where: { userId },
      orderBy: { timestamp: 'desc' },
    })
    return list.map(m => ({
      ...m,
      timestamp: Number(m.timestamp),
    }))
  }
}
