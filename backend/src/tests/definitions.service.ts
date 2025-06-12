import { Injectable } from '@nestjs/common'
import { PrismaService } from 'src/prisma.service'
import { CreateTestDefinitionDto } from './dto/create-test-definition.dto'

@Injectable()
export class DefinitionsService {
  constructor(private prisma: PrismaService) {}

  async findAll(userId: number) {
    const defs = await this.prisma.testDefinition.findMany({
      where: { userId },
      orderBy: { createdAt: 'desc' },
    })
    return defs.map(d => ({
      id: d.id,
      name: d.name,
      tests: d.tests as string[],
    }))
  }

  create(userId: number, dto: CreateTestDefinitionDto) {
    return this.prisma.testDefinition.create({
      data: {
        userId,
        name: dto.name,
        tests: dto.tests,
      },
      select: { id: true, name: true, tests: true },
    })
  }
}
