import {
  Controller,
  Post,
  Get,
  Body,
  Req,
  UseGuards,
  HttpCode,
} from '@nestjs/common'
import { JwtGuard } from '../auth/guards/jwt.guard'
import { MeasurementsService } from './measurements.service'
import {
  ApiTags,
  ApiBearerAuth,
  ApiOperation,
  ApiResponse,
  ApiBody,
} from '@nestjs/swagger'
import { CreateMeasurementDto } from './dto/create-measurements.dto'

@ApiTags('Measurements')
@Controller('measurements')
export class MeasurementsController {
  constructor(private readonly svc: MeasurementsService) {}

  @Post()
  @UseGuards(JwtGuard)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Add a new measurement record' })
  @ApiBody({ type: CreateMeasurementDto })
  @ApiResponse({ status: 201, description: 'Measurement added successfully' })
  @ApiResponse({ status: 400, description: 'Invalid request body' })
  @ApiResponse({ status: 401, description: 'Unauthorized' })
  @HttpCode(201)
  create(@Req() req, @Body() dto: CreateMeasurementDto) {
    return this.svc.create(req.user.userId, dto)
  }

  @Get()
  @UseGuards(JwtGuard)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Get all measurements for the authenticated user' })
  @ApiResponse({
    status: 200,
    description: 'List of measurements',
    schema: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          id: { type: 'integer', example: 1 },
          timestamp: { type: 'integer', example: 1627890123456 },
          n: { type: 'integer', example: 1 },
          s: { type: 'integer', example: 1 },
          t: { type: 'integer', example: 1 },
          band: { type: 'integer', example: 3 },
          ulArfcn: { type: 'integer', example: 1800 },
          dlArfcn: { type: 'integer', example: 1800 },
          code: { type: 'integer', example: 100 },
          ulBw: { type: 'number', example: 10.5 },
          dlBw: { type: 'number', example: 20.2 },
          plmnId: { type: 'integer', example: 12345 },
          tacOrLac: { type: 'integer', example: 54321 },
          rac: { type: 'integer', example: 1 },
          longCellId: { type: 'integer', example: 123 },
          siteId: { type: 'integer', example: 456 },
          cellId: { type: 'integer', example: 789 },
          latitude: { type: 'number', example: 35.6892 },
          longitude: { type: 'number', example: 51.3890 },
          signalStrength: { type: 'integer', example: -70 },
          networkType: { type: 'string', example: 'LTE' },
          downloadSpeed: { type: 'number', example: 50.5 },
          uploadSpeed: { type: 'number', example: 10.1 },
          pingTime: { type: 'integer', example: 30 },
        },
      },
    },
  })
  @ApiResponse({ status: 401, description: 'Unauthorized' })
  findAll(@Req() req) {
    return this.svc.findAll(req.user.userId)
  }
}
