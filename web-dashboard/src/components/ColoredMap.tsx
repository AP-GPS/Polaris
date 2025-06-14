import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, CircleMarker, useMapEvents, Popup } from 'react-leaflet';
import { Typography, Box, Button, Alert, CircularProgress } from '@mui/material';
import { Link } from 'react-router-dom';
import type { Measurement } from './Dashboard';
import { privateApi } from '../services/api';

export interface Point {
    coords: [number, number]; // [lat, lng]
    signalStrength: number;
    color: string;
}

const getSignalColor = (strength: number): string => {
    if (strength <= -120) return '#8B0000';     // Very Poor
    if (strength <= -110) return '#FF0000';     // Poor
    if (strength <= -100) return '#FF4500';     // Weak
    if (strength <= -90) return '#FFA500';      // Fair
    if (strength <= -80) return '#FFFF00';      // Good
    if (strength <= -70) return '#ADFF2F';      // Very Good
    if (strength <= -60) return '#00FF00';      // Excellent
    return '#006400';                           // Perfect
};

// const mockPoints: Point[] = [
//     {
//         coords: [35.6892, 51.3890], // مرکز تهران
//         signalStrength: -65,
//         color: getSignalColor(-65),
//     },
//     {
//         coords: [35.7324, 51.4225], // تجریش
//         signalStrength: -80,
//         color: getSignalColor(-80),
//     },
//     {
//         coords: [35.7079, 51.3512], // مرزداران
//         signalStrength: -95,
//         color: getSignalColor(-95),
//     },
//     {
//         coords: [35.7736, 51.2761], // شهران
//         signalStrength: -110,
//         color: getSignalColor(-110),
//     },
//     {
//         coords: [35.6652, 51.4394], // خیابان شریعتی
//         signalStrength: -70,
//         color: getSignalColor(-70),
//     },
//     {
//         coords: [35.6218, 51.3810], // نازی‌آباد
//         signalStrength: -100,
//         color: getSignalColor(-100),
//     },
//     {
//         coords: [35.7380, 51.4742], // لویزان
//         signalStrength: -55,
//         color: getSignalColor(-55),
//     },
// ];

const ClickHandler: React.FC<{ onClick: (lat: number, lng: number) => void }> = ({ onClick }) => {
    useMapEvents({
        click(e) {
            onClick(e.latlng.lat, e.latlng.lng);
        },
    });
    return null;
};

const ColoredMap: React.FC = () => {
    const [clickedCoords, setClickedCoords] = useState<[number, number] | null>(null);
    const [points, setPoints] = useState<Point[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const fetchData = async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await privateApi.get<Measurement[]>("/measurements");
            const pts: Point[] = res.data.map(d => ({
                coords: [d.latitude, d.longitude],
                signalStrength: d.signalStrength,
                color: getSignalColor(d.signalStrength)
            }));
            setPoints(pts);
        } catch (err: any) {
            setError(err.response?.data?.message || "خطا در واکشی داده‌ها");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        // setPoints(mockPoints);
        fetchData();
    }, []);

    if (loading) {
        return <CircularProgress />
    }

    return (
        <Box
            sx={{
                width: '100vw',
                height: '100vh',
                bgcolor: 'grey.100',
                position: 'relative'
            }}
        >
            {error && (
                <Alert severity="error">
                    {error}
                </Alert>
            )}

            <Link to={'/dashboard'}>
                <Button variant='contained' sx={{
                    position: 'absolute',
                    top: error ? 60 : 10,
                    right: 10,
                    zIndex: 1000,
                    boxShadow: 'none',
                    fontWeight: 'bold',
                    px: 3,
                    fontSize: 18,
                }}>
                    بازگشت
                </Button>
            </Link>

            <MapContainer
                center={[35.6892, 51.3890]} // center of Tehran for default location
                zoom={7}
                style={{ height: '100%', width: '100%', position: 'relative' }}
            >
                <TileLayer
                    attribution='&copy; <a href="https://osm.org/">OpenStreetMap</a> contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                {points.map((pt, idx) => (
                    <CircleMarker
                        key={idx}
                        center={pt.coords}
                        radius={10}
                        pathOptions={{
                            color: pt.color,
                            fillColor: pt.color,
                            fillOpacity: 0.6
                        }}
                    >
                        <Popup>
                            <Typography sx={{ direction: 'rtl' }} variant="body2">
                                مختصات: {pt.coords[0].toFixed(4)}, {pt.coords[1].toFixed(4)}<br />
                                قدرت سیگنال: {pt.signalStrength} dBm
                            </Typography>
                        </Popup>
                    </CircleMarker>
                ))}

                <ClickHandler onClick={(lat, lng) => setClickedCoords([lat, lng])} />
            </MapContainer>

            {clickedCoords && (
                <Box sx={{ mt: 2, p: 1, border: '1px solid', borderColor: 'grey.300', borderRadius: 1 }}>
                    <Typography variant="subtitle1">مختصات کلیک‌شده:</Typography>
                    <Typography>
                        عرض جغرافیایی: {clickedCoords[0].toFixed(4)}, طول جغرافیایی: {clickedCoords[1].toFixed(4)}
                    </Typography>
                </Box>
            )}
        </Box>
    );
};

export default ColoredMap;
