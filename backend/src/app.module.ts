import { Module } from '@nestjs/common'
import { AuthModule } from './auth/auth.module'
import { ConfigModule } from './config/config.module'
import { MeasurementsModule } from './measurements/measurements.module'
import { TestsModule } from './tests/tests.module'
import { PrismaService } from './prisma.service'

@Module({
  imports: [
    AuthModule,
    ConfigModule,
    MeasurementsModule,
    TestsModule,
  ],
  providers: [PrismaService],
})
export class AppModule {}
