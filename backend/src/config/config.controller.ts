import {
  Controller,
  Get,
  Patch,
  Body,
  UseGuards,
} from '@nestjs/common'
import { ConfigService } from './config.service'
import { UpdateConfigDto } from './dto/update-config.dto'
import { JwtGuard } from '../auth/guards/jwt.guard'
import {
  ApiTags,
  ApiBearerAuth,
  ApiOperation,
  ApiResponse,
  ApiBody,
} from '@nestjs/swagger'

@ApiTags('Config')
@Controller('config')
export class ConfigController {
  constructor(private readonly configService: ConfigService) {}

  @Get()
  @UseGuards(JwtGuard)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Fetch current user configuration' })
  @ApiResponse({ status: 200, description: 'Configuration retrieved successfully' })
  @ApiResponse({ status: 401, description: 'Unauthorized access' })
  fetchConfig() {
    return this.configService.fetch()
  }

  @Patch()
  @UseGuards(JwtGuard)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Update user configuration' })
  @ApiBody({ type: UpdateConfigDto })
  @ApiResponse({ status: 200, description: 'Configuration updated successfully' })
  @ApiResponse({ status: 400, description: 'Invalid request body' })
  @ApiResponse({ status: 401, description: 'Unauthorized access' })
  updateConfig(@Body() dto: UpdateConfigDto) {
    return this.configService.update(dto)
  }
}
