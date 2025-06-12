import {
  Controller,
  Get,
  Post,
  Body,
  Req,
  UseGuards,
  HttpCode,
} from '@nestjs/common'
import {
  ApiTags,
  ApiBearerAuth,
  ApiOperation,
  ApiResponse,
  ApiBody,
} from '@nestjs/swagger'
import { JwtGuard } from '../auth/guards/jwt.guard'
import { DefinitionsService } from './definitions.service'
import { CreateTestDefinitionDto } from './dto/create-test-definition.dto'

@ApiTags('Test Definitions')
@ApiBearerAuth()
@UseGuards(JwtGuard)
@Controller('tests/definitions')
export class DefinitionsController {
  constructor(private readonly svc: DefinitionsService) {}

  @Get()
  @ApiOperation({ summary: 'Fetch defined tests for the authenticated user' })
  @ApiResponse({
    status: 200,
    description: 'List of test definitions',
    schema: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          id: { type: 'integer', example: 1 },
          name: { type: 'string', example: 'Basic Network Test' },
          tests: {
            type: 'array',
            items: { type: 'string' },
            example: ['ping', 'download'],
          },
        },
      },
    },
  })
  @ApiResponse({ status: 401, description: 'Unauthorized (missing or invalid token)' })
  @HttpCode(200)
  findAll(@Req() req) {
    return this.svc.findAll(req.user.userId)
  }

  @Post()
  @ApiOperation({ summary: 'Create a new test definition' })
  @ApiBody({ type: CreateTestDefinitionDto })
  @ApiResponse({
    status: 201,
    description: 'Test definition created',
    schema: {
      type: 'object',
      properties: {
        id: { type: 'integer', example: 2 },
        name: { type: 'string', example: 'Quick Check' },
        tests: {
          type: 'array',
          items: { type: 'string' },
          example: ['ping', 'upload'],
        },
      },
    },
  })
  @ApiResponse({ status: 400, description: 'Invalid request body' })
  @ApiResponse({ status: 401, description: 'Unauthorized (missing or invalid token)' })
  @HttpCode(201)
  create(@Req() req, @Body() dto: CreateTestDefinitionDto) {
    return this.svc.create(req.user.userId, dto)
  }
}
