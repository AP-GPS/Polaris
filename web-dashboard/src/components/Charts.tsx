import React, { useEffect, useState } from 'react';
import { Container, Typography, Box, Button, Grid, Alert } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { privateApi } from '../services/api';
import type { Measurement } from './Dashboard';
import CustomPieChart from './PieChart';
import CustomLineChart from './LineChart';
import PingLineChart from './PingLineChart';

const COLORS = [
    'rgb(46, 47, 47)',
    'rgb(53, 70, 84)',
    'rgb(67, 95, 120)',
    'rgb(108, 143, 173)',
    'rgb(142, 157, 172)',
    'rgb(162, 176, 182)',
];

// const mockMeasurements: Measurement[] = [
//     {
//         id: 1,
//         userId: 101,
//         timestamp: 1718000000000,
//         n: 100,
//         s: 'session1',
//         t: 'LTE',
//         band: 'LTE',
//         ulArfcn: 18100,
//         dlArfcn: 6350,
//         code: 101,
//         ulBw: 20,
//         dlBw: 20,
//         plmnId: 43211,
//         tacOrLac: 10001,
//         rac: 1,
//         longCellId: 111000001,
//         siteId: 2001,
//         cellId: 1,
//         latitude: 35.6892,
//         longitude: 51.3890,
//         signalStrength: -85,
//         networkType: '4G',
//         downloadSpeed: 25000,
//         uploadSpeed: 8000,
//         pingTime: 30
//     },
//     {
//         id: 2,
//         userId: 102,
//         timestamp: 1718000100000,
//         n: 101,
//         s: 'session2',
//         t: 'NR',
//         band: 'NR',
//         ulArfcn: 23000,
//         dlArfcn: 3600,
//         code: 102,
//         ulBw: 100,
//         dlBw: 100,
//         plmnId: 43211,
//         tacOrLac: 10002,
//         rac: 2,
//         longCellId: 111000002,
//         siteId: 2002,
//         cellId: 2,
//         latitude: 35.7001,
//         longitude: 51.4001,
//         signalStrength: -70,
//         networkType: '5G',
//         downloadSpeed: 100000,
//         uploadSpeed: 25000,
//         pingTime: 10
//     },
//     {
//         id: 3,
//         userId: 103,
//         timestamp: 1718000200000,
//         n: 102,
//         s: 'session3',
//         t: 'UMTS',
//         band: 'WCDMA',
//         ulArfcn: 19500,
//         dlArfcn: 10562,
//         code: 103,
//         ulBw: 5,
//         dlBw: 5,
//         plmnId: 43211,
//         tacOrLac: 10003,
//         rac: 3,
//         longCellId: 111000003,
//         siteId: 2003,
//         cellId: 3,
//         latitude: 35.7102,
//         longitude: 51.4020,
//         signalStrength: -90,
//         networkType: '3G',
//         downloadSpeed: 4500,
//         uploadSpeed: 1200,
//         pingTime: 45
//     },
//     {
//         id: 4,
//         userId: 104,
//         timestamp: 1718000300000,
//         n: 103,
//         s: 'session4',
//         t: 'LTE',
//         band: 'LTE',
//         ulArfcn: 18150,
//         dlArfcn: 6400,
//         code: 104,
//         ulBw: 15,
//         dlBw: 20,
//         plmnId: 43211,
//         tacOrLac: 10004,
//         rac: 1,
//         longCellId: 111000004,
//         siteId: 2004,
//         cellId: 4,
//         latitude: 35.7200,
//         longitude: 51.4100,
//         signalStrength: -80,
//         networkType: '4G',
//         downloadSpeed: 30000,
//         uploadSpeed: 7000,
//         pingTime: 28
//     },
//     {
//         id: 5,
//         userId: 105,
//         timestamp: 1718000400000,
//         n: 104,
//         s: 'session5',
//         t: 'NR',
//         band: 'NR',
//         ulArfcn: 23500,
//         dlArfcn: 3700,
//         code: 105,
//         ulBw: 80,
//         dlBw: 100,
//         plmnId: 43211,
//         tacOrLac: 10005,
//         rac: 2,
//         longCellId: 111000005,
//         siteId: 2005,
//         cellId: 5,
//         latitude: 35.7300,
//         longitude: 51.4200,
//         signalStrength: -60,
//         networkType: '5G',
//         downloadSpeed: 120000,
//         uploadSpeed: 30000,
//         pingTime: 12
//     },
//     {
//         id: 6,
//         userId: 106,
//         timestamp: 1718000500000,
//         n: 105,
//         s: 'session6',
//         t: 'LTE',
//         band: 'LTE',
//         ulArfcn: 18200,
//         dlArfcn: 6450,
//         code: 106,
//         ulBw: 10,
//         dlBw: 10,
//         plmnId: 43211,
//         tacOrLac: 10006,
//         rac: 1,
//         longCellId: 111000006,
//         siteId: 2006,
//         cellId: 6,
//         latitude: 35.7400,
//         longitude: 51.4300,
//         signalStrength: -88,
//         networkType: '4G',
//         downloadSpeed: 22000,
//         uploadSpeed: 6000,
//         pingTime: 35
//     },
//     {
//         id: 7,
//         userId: 107,
//         timestamp: 1718000600000,
//         n: 106,
//         s: 'session7',
//         t: 'UMTS',
//         band: 'WCDMA',
//         ulArfcn: 19600,
//         dlArfcn: 10563,
//         code: 107,
//         ulBw: 5,
//         dlBw: 5,
//         plmnId: 43211,
//         tacOrLac: 10007,
//         rac: 3,
//         longCellId: 111000007,
//         siteId: 2007,
//         cellId: 7,
//         latitude: 35.7500,
//         longitude: 51.4400,
//         signalStrength: -95,
//         networkType: '3G',
//         downloadSpeed: 4000,
//         uploadSpeed: 1100,
//         pingTime: 50
//     },
//     {
//         id: 8,
//         userId: 108,
//         timestamp: 1718000700000,
//         n: 107,
//         s: 'session8',
//         t: 'NR',
//         band: 'NR',
//         ulArfcn: 24000,
//         dlArfcn: 3800,
//         code: 108,
//         ulBw: 100,
//         dlBw: 100,
//         plmnId: 43211,
//         tacOrLac: 10008,
//         rac: 2,
//         longCellId: 111000008,
//         siteId: 2008,
//         cellId: 8,
//         latitude: 35.7600,
//         longitude: 51.4500,
//         signalStrength: -65,
//         networkType: '5G',
//         downloadSpeed: 110000,
//         uploadSpeed: 27000,
//         pingTime: 14
//     },
//     {
//         id: 9,
//         userId: 109,
//         timestamp: 1718000800000,
//         n: 108,
//         s: 'session9',
//         t: 'LTE',
//         band: 'LTE',
//         ulArfcn: 18300,
//         dlArfcn: 6500,
//         code: 109,
//         ulBw: 20,
//         dlBw: 20,
//         plmnId: 43211,
//         tacOrLac: 10009,
//         rac: 1,
//         longCellId: 111000009,
//         siteId: 2009,
//         cellId: 9,
//         latitude: 35.7700,
//         longitude: 51.4600,
//         signalStrength: -78,
//         networkType: '4G',
//         downloadSpeed: 28000,
//         uploadSpeed: 9000,
//         pingTime: 26
//     },
//     {
//         id: 10,
//         userId: 110,
//         timestamp: 1718000900000,
//         n: 109,
//         s: 'session10',
//         t: 'NR',
//         band: 'NR',
//         ulArfcn: 24500,
//         dlArfcn: 3900,
//         code: 110,
//         ulBw: 90,
//         dlBw: 90,
//         plmnId: 43211,
//         tacOrLac: 10010,
//         rac: 2,
//         longCellId: 111000010,
//         siteId: 2010,
//         cellId: 10,
//         latitude: 35.7800,
//         longitude: 51.4700,
//         signalStrength: -72,
//         networkType: '5G',
//         downloadSpeed: 105000,
//         uploadSpeed: 26000,
//         pingTime: 11
//     },
//     {
//         id: 11,
//         userId: 111,
//         timestamp: 1718001000000,
//         n: 110,
//         s: 'session11',
//         t: 'UMTS',
//         band: 'WCDMA',
//         ulArfcn: 19700,
//         dlArfcn: 10564,
//         code: 111,
//         ulBw: 5,
//         dlBw: 5,
//         plmnId: 43211,
//         tacOrLac: 10011,
//         rac: 3,
//         longCellId: 111000011,
//         siteId: 2011,
//         cellId: 11,
//         latitude: 35.7900,
//         longitude: 51.4800,
//         signalStrength: -93,
//         networkType: '3G',
//         downloadSpeed: 4700,
//         uploadSpeed: 1300,
//         pingTime: 48
//     },
//     {
//         id: 12,
//         userId: 112,
//         timestamp: 1718001100000,
//         n: 111,
//         s: 'session12',
//         t: 'LTE',
//         band: 'LTE',
//         ulArfcn: 18400,
//         dlArfcn: 6550,
//         code: 112,
//         ulBw: 20,
//         dlBw: 20,
//         plmnId: 43211,
//         tacOrLac: 10012,
//         rac: 1,
//         longCellId: 111000012,
//         siteId: 2012,
//         cellId: 12,
//         latitude: 35.8000,
//         longitude: 51.4900,
//         signalStrength: -82,
//         networkType: '4G',
//         downloadSpeed: 24000,
//         uploadSpeed: 7500,
//         pingTime: 32
//     }
// ];

const Charts: React.FC = () => {
    const [data, setData] = useState<Measurement[]>([]);
    const [error, setError] = useState<string | null>(null);
    const navigate = useNavigate();

    useEffect(() => {
        // setData(mockMeasurements); // For testing with mock data
        // return;

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
                {error && (
                    <Alert severity="error" sx={{ mb: 2 }}>
                        {error}
                    </Alert>
                )}
                <Box display='flex' gap={1} flexWrap={'wrap'} justifyContent={'space-between'}>
                    <CustomPieChart
                        colors={COLORS}
                        techData={techData}
                        title="توزیع تعداد تست‌ها بر اساس نوع شبکه"
                        labelFunc={(name, percent) => `${name}: ${(percent * 100).toFixed(1)}%`}
                    />
                    <CustomPieChart
                        colors={COLORS}
                        techData={arfcnData}
                        title="توزیع ARFCN‌ های پراستفاده در شبکه 4G"
                        labelFunc={(name, percent) => `ARFCN: ${name} (${(percent * 100).toFixed(1)}%)`}
                    />
                </Box>
                <CustomLineChart
                    data={data}
                    title="نمودار زمانی سرعت دانلود و آپلود (kbps)"
                />
                <PingLineChart 
                    data={data}
                    title="نمودار زمانی پینگ تست‌ها (ms)"
                />
            </Container>
        </Grid>
    );
};

export default Charts;