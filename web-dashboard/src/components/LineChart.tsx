import { Typography, Paper } from '@mui/material';
import {
    Tooltip,
    Legend,
    ResponsiveContainer,
    LineChart,
    CartesianGrid,
    XAxis,
    YAxis,
    Line,
} from 'recharts';
import dayjs from 'dayjs';
import type { Measurement } from './Dashboard';

const CustomLineChart = ({ data, title }: { data: Measurement[], title: string }) => {
    return (
        <Paper sx={{ height: { xs: 300, sm: 520 }, mb: 1, boxShadow: 4, display: 'flex', flexDirection: 'column', width: '100%' }}>
            <Typography sx={{ direction: 'rtl' }} textAlign='start' pr={2} pt={1} variant="subtitle1" align="center" gutterBottom>
                {title}
            </Typography>
            <ResponsiveContainer width="100%" height="100%">
                <LineChart data={data.map(m => ({
                    timestamp: dayjs(m.timestamp).format('DD:HH:mm'),
                    downloadSpeed: m.downloadSpeed,
                    uploadSpeed: m.uploadSpeed,
                }))}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis tick={{ fontSize: 13 }} dataKey="timestamp" />
                    <YAxis tick={{ fontSize: 13 }} />
                    <Tooltip />
                    <Legend />
                    <Line type="monotone" dataKey="downloadSpeed" name="سرعت دانلود (kbps)" stroke="#8884d8" strokeWidth={2} dot />
                    <Line type="monotone" dataKey="uploadSpeed" name="سرعت آپلود (kbps)" stroke="#82ca9d" strokeWidth={2} dot />
                </LineChart>
            </ResponsiveContainer>
        </Paper>
    )
}

export default CustomLineChart;