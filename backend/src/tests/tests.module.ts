import { Module } from '@nestjs/common'
import { RunController } from './run.controller'
import { DefinitionsController } from './definitions.controller'
import { DefinitionsService } from './definitions.service'
import { PrismaService } from 'src/prisma.service'

@Module({
  controllers: [DefinitionsController, RunController],
  providers: [DefinitionsService, PrismaService],
})
export class TestsModule {}
