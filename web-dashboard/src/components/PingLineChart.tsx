import {
    LineChart,
    Line,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Legend,
    ResponsiveContainer,
    ReferenceLine
} from 'recharts';
import { Typography, Paper } from '@mui/material';
import dayjs from 'dayjs';
import type { Measurement } from './Dashboard';

const PingLineChart = ({ data, title }: { data: Measurement[], title: string }) => {
    const chartData = data.map((m) => ({
        ...m,
        formattedTime: dayjs(m.timestamp).format('YY-MM-DD HH:mm'),
        color:
            m.pingTime > 300 ? 'red' :
                m.pingTime > 150 ? 'orange' :
                    m.pingTime > 100 ? 'yellow' :
                        'green'
    }));

    return (
        <Paper sx={{ height: { xs: 370, sm: 550 }, mb: 2, boxShadow: 4, display: 'flex', flexDirection: 'column', width: '100%' }}>
            <Typography sx={{ direction: 'rtl' }} textAlign='start' pr={2} pt={1} variant="subtitle1" align="center" gutterBottom>
                {title}
            </Typography>
            <ResponsiveContainer width="100%" height="100%">
                <LineChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis tick={{ fontSize: 13 }} dataKey="formattedTime" angle={-45} textAnchor="end" height={100} />
                    <YAxis tick={{ fontSize: 13 }} label={{ value: 'پینگ (ms)', angle: -90, position: 'insideLeft' }} />
                    <Tooltip formatter={(value: number) => `${value} ms`} />
                    <Legend />
                    <ReferenceLine y={300} stroke="blue" strokeDasharray="3 3" label="حد آستانه 300ms" />
                    <Line
                        type="monotone"
                        dataKey="pingTime"
                        stroke="#888"
                        strokeWidth={1}
                        dot={({ cx, cy, payload }) => (
                            <circle
                                cx={cx}
                                cy={cy}
                                r={4}
                                fill={payload.color}
                            />
                        )}
                        name="پینگ (ms)"
                    />
                </LineChart>
            </ResponsiveContainer>
        </Paper>
    );
};

export default PingLineChart;
