import { Box, Grid } from "@mui/material";
import ColoredMap, { type Point } from "./ColoredMap";

// const points: Point[] = [
//     { coords: [35.6892, 51.3890], color: 'red' },
//     { coords: [34.6399, 50.8759], color: 'blue' },
//     { coords: [31.8974, 54.3569], color: 'green' },
// ];

const Map = () => {
    return (
        <Grid sx={{
            border: 'solid 3px',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            width: '100vw',
            height: '100vh'
        }}>
            <Box >
                <ColoredMap />
            </Box>
        </Grid>
    )
}

export default Map;