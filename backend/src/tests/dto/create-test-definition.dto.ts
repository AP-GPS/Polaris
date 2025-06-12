import { IsString, IsArray, ArrayNotEmpty } from 'class-validator'
import { ApiProperty } from '@nestjs/swagger'

export class CreateTestDefinitionDto {
  @ApiProperty({
    description: 'Name of the test definition',
    example: 'Quick Check',
  })
  @IsString()
  name: string

  @ApiProperty({
    description: 'Array of test keys to include',
    example: ['ping', 'upload'],
    type: [String],
  })
  @IsArray()
  @ArrayNotEmpty()
  @IsString({ each: true })
  tests: string[]
}
