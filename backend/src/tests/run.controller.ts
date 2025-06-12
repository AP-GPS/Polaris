import {
  Controller,
  Get,
  Post,
  Res,
  UploadedFile,
  UseInterceptors,
  BadRequestException,
  HttpCode,
} from '@nestjs/common'
import { FileInterceptor } from '@nestjs/platform-express'
import { ApiTags, ApiOperation, ApiResponse, ApiConsumes, ApiBody } from '@nestjs/swagger'
import { Response } from 'express'
import { createReadStream, statSync } from 'fs'
import { join } from 'path'

@ApiTags('Test Execution')
@Controller('tests/run')
export class RunController {
  @Get('ping')
  @ApiOperation({ summary: 'Perform a ping test' })
  @ApiResponse({ status: 200, description: 'Ping test result' })
  @HttpCode(200)
  ping(): void {}

  @Get('download')
  @ApiOperation({ summary: 'Perform a download speed test (returns a binary file)' })
  @ApiResponse({ status: 200, description: 'Binary file for download test', schema: { type: 'string', format: 'binary' } })
  download(@Res() res: Response): void {
    const filePath = join(__dirname, '../../utils/testfile.bin')
    const stat = statSync(filePath)
    res.set({
      'Content-Type': 'application/octet-stream',
      'Content-Length': stat.size,
    })
    createReadStream(filePath).pipe(res)
  }

  @Post('upload')
  @ApiOperation({ summary: 'Perform an upload speed test (client uploads a file)' })
  @ApiConsumes('multipart/form-data')
  @ApiBody({
    schema: {
      type: 'object',
      properties: {
        file: { type: 'string', format: 'binary', description: 'File to upload for speed test' },
      },
    },
  })
  @ApiResponse({ status: 200, description: 'Upload test succeeded' })
  @ApiResponse({ status: 400, description: 'Bad request (no file received)' })
  @UseInterceptors(FileInterceptor('file'))
  upload(@UploadedFile() file: Express.Multer.File): void {
    if (!file) throw new BadRequestException('File is required')
  }
}
