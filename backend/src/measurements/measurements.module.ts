import { Module } from '@nestjs/common'
import { MeasurementsService } from './measurements.service'
import { MeasurementsController } from './measurements.controller'
import { PrismaService } from 'src/prisma.service'

@Module({
  providers: [MeasurementsService, PrismaService],
  controllers: [MeasurementsController],
})
export class MeasurementsModule {}
