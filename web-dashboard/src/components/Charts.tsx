import React, { useEffect, useState } from 'react';
import { Container, Typography, Box, Button, Grid, Paper } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { privateApi } from '../services/api';
import {
    PieChart,
    Pie,
    Cell,
    Tooltip,
    Legend,
    ResponsiveContainer,
} from 'recharts';

interface Measurement {
    id: number;
    timestamp: number;
    networkType: string;
    dlArfcn: number;
    downloadSpeed: number;
    uploadSpeed: number;
}

const COLORS = [
    'rgb(46, 47, 47)',
    'rgb(53, 70, 84)',
    'rgb(67, 95, 120)',
    'rgb(108, 143, 173)',
    'rgb(142, 157, 172)',
    'rgb(162, 176, 182)',
];

const Charts: React.FC = () => {
    const [data, setData] = useState<Measurement[]>([]);
    const [error, setError] = useState<string | null>(null);
    const navigate = useNavigate();

    useEffect(() => {
        async function fetchData() {
            try {
                const res = await privateApi.get<Measurement[]>('/measurements');
                setData(res.data);
            } catch (err: any) {
                setError(err.response?.data?.message || 'خطا در دریافت داده‌ها');
            }
        }
        fetchData();
    }, []);

    const techCounts = data.reduce((acc, m) => {
        acc[m.networkType] = (acc[m.networkType] || 0) + 1;
        return acc;
    }, {} as Record<string, number>);
    const techData = Object.entries(techCounts).map(([name, value]) => ({ name, value }));

    const arfcnDataMap = data
        .filter(m => m.networkType === '4G')
        .reduce((acc, m) => {
            acc[m.dlArfcn] = (acc[m.dlArfcn] || 0) + 1;
            return acc;
        }, {} as Record<number, number>);
    const topARFCN = Object.entries(arfcnDataMap)
        .sort(([, a], [, b]) => b - a)
        .slice(0, 6);
    const arfcnData = topARFCN.map(([dlArfcn, value]) => ({ name: dlArfcn, value }));

    return (
        <Grid width="100vw">
            <Container maxWidth="lg" sx={{ my: 2 }}>
                <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                    <Button variant="outlined" onClick={() => navigate('/')}>بازگشت</Button>
                    <Typography variant="h5">نمودار نتایج تست‌ها</Typography>
                </Box>
                {error && <Typography color="error">{error}</Typography>}
                <Box display='flex' gap={2} flexWrap={'wrap'}>
                    <Paper sx={{ height: 400, mb: 1, boxShadow: 4, display: 'flex', flexDirection: 'column', width: { xs: '100%', md: '49%' } }}>
                        <Typography sx={{ direction: 'rtl' }} textAlign='start' pr={2} pt={1} variant="subtitle1" align="center" gutterBottom>
                            توزیع تعداد تست‌ها بر اساس نوع شبکه
                        </Typography>
                        <ResponsiveContainer>
                            <PieChart>
                                <Pie
                                    data={techData}
                                    dataKey="value"
                                    nameKey="name"
                                    outerRadius={100}
                                    fontSize={10}
                                    label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(1)}%`}
                                >
                                    {techData.map((_, idx) => (
                                        <Cell key={idx} fill={COLORS[idx % COLORS.length]} />
                                    ))}
                                </Pie>
                                <Tooltip />
                                <Legend />
                            </PieChart>
                        </ResponsiveContainer>
                    </Paper>
                    <Paper sx={{ height: 400, mb: 1, boxShadow: 4, display: 'flex', flexDirection: 'column', width: { xs: '100%', md: '49%' } }}>
                        <Typography sx={{ direction: 'rtl' }} textAlign='start' pr={2} pt={1} variant="subtitle1" align="center" gutterBottom>
                            توزیع ARFCN‌ های پراستفاده در شبکه 4G
                        </Typography>
                        <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                                <Pie
                                    data={arfcnData}
                                    dataKey="value"
                                    nameKey="name"
                                    outerRadius={100}
                                    fontSize={12}
                                    label={({ name, /* percent */ }) => `ARFCN: ${name}`}
                                >
                                    {arfcnData.map((_, idx) => (
                                        <Cell key={idx} fill={COLORS[(idx + 1) % COLORS.length]} />
                                    ))}
                                </Pie>
                                <Tooltip />
                                <Legend />
                            </PieChart>
                        </ResponsiveContainer>
                    </Paper>
                </Box>
            </Container>
        </Grid>
    );
};

export default Charts;