import { Paper, Typography } from "@mui/material"
import {
    PieChart,
    Pie,
    Cell,
    Tooltip,
    Legend,
    ResponsiveContainer,
} from 'recharts';

const CustomPieChart = ({ techData, colors, title, labelFunc }: { techData: { name: string; value: number }[], colors: string[], title: string, labelFunc: (name: string, percent: number) => string }) => {
    return (
        <Paper sx={{ height: 400, mb: 1, boxShadow: 4, display: 'flex', flexDirection: 'column', width: { xs: '100%', md: '49.4%' } }}>
            <Typography sx={{ direction: 'rtl' }} textAlign='start' pr={2} pt={1} variant="subtitle1" align="center" gutterBottom>
                {title}
            </Typography>
            <ResponsiveContainer>
                <PieChart>
                    <Pie
                        data={techData}
                        dataKey="value"
                        nameKey="name"
                        outerRadius={100}
                        fontSize={10}
                        label={({ name, percent }) => labelFunc(name, percent)}
                    >
                        {techData.map((_, idx) => (
                            <Cell key={idx} fill={colors[idx % colors.length]} />
                        ))}
                    </Pie>
                    <Tooltip />
                    <Legend />
                </PieChart>
            </ResponsiveContainer>
        </Paper>
    )
}

export default CustomPieChart;