use core::f64::consts::PI;

pub struct Tile {
    x: i64,
    y: i64,
    z: i64,
}

type BoundingBox = [[f64; 2]; 2];

fn xy_from_latitude_longitude(latitude: f64, longitude: f64, zoom: i64) -> (i64, i64) {
    let n = 2.0_f64.powf(zoom as f64);
    (
        ((longitude + 180.0) / 360.0 * n).floor() as i64,
        ((1.0 - latitude.to_radians().tan().asinh() / PI) / 2.0 * n).floor() as i64,
    )
}

fn get_nw_se_coordinates(zoom: i64, bbox: BoundingBox) -> (i64, i64, i64, i64) {
    let (nw_x, nw_y) = xy_from_latitude_longitude(bbox[0][1], bbox[0][0], zoom);
    let (se_x, se_y) = xy_from_latitude_longitude(bbox[1][1], bbox[1][0], zoom);
    // Panic if fails TODO check if it is the expected behavior
    assert!(nw_x <= se_x);
    assert!(se_y <= nw_y);
    (nw_x, nw_y, se_x, se_y)
}

pub fn get_tiles_to_invalidate(max_zoom: i64, bbox: BoundingBox) -> Vec<Tile> {
    let mut affected_tiles: Vec<Tile> = Vec::new();
    for zoom in 0..(max_zoom + 1) {
        let (nw_x, nw_y, se_x, se_y) = get_nw_se_coordinates(zoom, bbox);
        for x in nw_x..(se_x + 1) {
            for y in se_y..(nw_y + 1) {
                affected_tiles.push(Tile { x, y, z: zoom })
            }
        }
    }
    affected_tiles
}

pub fn count_tiles(max_zoom: i64, bbox: BoundingBox) -> i64 {
    let mut count = 0;
    for zoom in 0..(max_zoom + 1) {
        let (nw_x, nw_y, se_x, se_y) = get_nw_se_coordinates(zoom, bbox);
        count += (se_x - nw_x) * (nw_y - se_y);
    }
    count
}

#[cfg(test)]
mod tests {
    use super::get_tiles_to_invalidate;

    #[test]
    fn find_tiles_to_invalidate() {
        let campus_sncf_bbox = [[2.3535, 48.921], [2.3568, 48.922]];
        let expected_tiles: Vec<(i64, i64, i64)> = vec![
            (0, 0, 0),
            (1, 0, 1),
            (2, 1, 2),
            (4, 2, 3),
            (8, 5, 4),
            (16, 11, 5),
            (32, 22, 6),
            (64, 44, 7),
            (129, 88, 8),
            (259, 176, 9),
            (518, 352, 10),
            (1037, 704, 11),
            (2074, 1408, 12),
            (4149, 2816, 13),
            (8299, 5632, 14),
            (16598, 11264, 15),
            (33196, 22528, 16),
            (33197, 22528, 16),
            (66392, 45056, 17),
            (66393, 45056, 17),
            (66394, 45056, 17),
            (132785, 90112, 18),
            (132785, 90113, 18),
            (132786, 90112, 18),
            (132786, 90113, 18),
            (132787, 90112, 18),
            (132787, 90113, 18),
            (132788, 90112, 18),
            (132788, 90113, 18),
        ];
        let mut found_tiles: Vec<(i64, i64, i64)> = Vec::new();
        for found_tile in get_tiles_to_invalidate(18, campus_sncf_bbox) {
            found_tiles.push((found_tile.x, found_tile.y, found_tile.z));
        }
        assert_eq!(expected_tiles, found_tiles);
    }
}
