import { Box, Grid } from "@mui/material";
import ColoredMap from "./ColoredMap";

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