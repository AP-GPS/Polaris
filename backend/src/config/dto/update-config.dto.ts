import { IsInt, Min } from 'class-validator'
import { ApiProperty } from '@nestjs/swagger'

export class UpdateConfigDto {
  @ApiProperty({
    description: 'فاصله زمانی بین اندازه‌گیری‌ها (بر حسب دقیقه)',
    example: 10,
    minimum: 0,
  })
  @IsInt()
  @Min(0)
  intervalMinutes: number

  @ApiProperty({
    description: 'آستانه قدرت سیگنال (مثلاً منفی برای dBm)',
    example: -80,
  })
  @IsInt()
  thresholdSignal: number

  @ApiProperty({
    description: 'حداقل سرعت دانلود مورد قبول (Mbps)',
    example: 20,
  })
  @IsInt()
  thresholdDownload: number

  @ApiProperty({
    description: 'حداقل سرعت آپلود مورد قبول (Mbps)',
    example: 5,
  })
  @IsInt()
  thresholdUpload: number

  @ApiProperty({
    description: 'حداکثر زمان مجاز پینگ (میلی‌ثانیه)',
    example: 100,
  })
  @IsInt()
  thresholdPing: number
}
