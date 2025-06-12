import { IsInt, IsNumber, IsString } from 'class-validator'
import { ApiProperty } from '@nestjs/swagger'

export class CreateMeasurementDto {
  @ApiProperty({ description: 'Unix timestamp of measurement', example: 1627890123456 })
  @IsInt()
  timestamp: number

  @ApiProperty({ description: 'Field n', example: 1 })
  @IsInt()
  n: number

  @ApiProperty({ description: 'Field s', example: 1 })
  @IsInt()
  s: number

  @ApiProperty({ description: 'Field t', example: 1 })
  @IsInt()
  t: number

  @ApiProperty({ description: 'Frequency band', example: 3 })
  @IsInt()
  band: number

  @ApiProperty({ description: 'Uplink ARFCN', example: 1800 })
  @IsInt()
  ulArfcn: number

  @ApiProperty({ description: 'Downlink ARFCN', example: 1800 })
  @IsInt()
  dlArfcn: number

  @ApiProperty({ description: 'Cell code', example: 100 })
  @IsInt()
  code: number

  @ApiProperty({ description: 'Uplink bandwidth (MHz)', example: 10.5 })
  @IsNumber()
  ulBw: number

  @ApiProperty({ description: 'Downlink bandwidth (MHz)', example: 20.2 })
  @IsNumber()
  dlBw: number

  @ApiProperty({ description: 'PLMN identifier', example: 12345 })
  @IsInt()
  plmnId: number

  @ApiProperty({ description: 'TAC or LAC', example: 54321 })
  @IsInt()
  tacOrLac: number

  @ApiProperty({ description: 'RAC', example: 1 })
  @IsInt()
  rac: number

  @ApiProperty({ description: 'Long Cell ID', example: 123 })
  @IsInt()
  longCellId: number

  @ApiProperty({ description: 'Site ID', example: 456 })
  @IsInt()
  siteId: number

  @ApiProperty({ description: 'Local Cell ID', example: 789 })
  @IsInt()
  cellId: number

  @ApiProperty({ description: 'Latitude of measurement', example: 35.6892 })
  @IsNumber()
  latitude: number

  @ApiProperty({ description: 'Longitude of measurement', example: 51.3890 })
  @IsNumber()
  longitude: number

  @ApiProperty({ description: 'Signal strength (dBm)', example: -70 })
  @IsInt()
  signalStrength: number

  @ApiProperty({ description: 'Network type (e.g. LTE)', example: 'LTE' })
  @IsString()
  networkType: string

  @ApiProperty({ description: 'Download speed (Mbps)', example: 50.5 })
  @IsNumber()
  downloadSpeed: number

  @ApiProperty({ description: 'Upload speed (Mbps)', example: 10.1 })
  @IsNumber()
  uploadSpeed: number

  @ApiProperty({ description: 'Ping time (ms)', example: 30 })
  @IsInt()
  pingTime: number
}
