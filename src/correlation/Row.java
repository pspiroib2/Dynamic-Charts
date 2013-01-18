/**
 * 
 */
package correlation;

class Row {
		double x, y, xy, x2, y2, difx, dify, difxy, difx2, dify2;
		
		Row(double xIn, double yIn, Row past) {
			x = xIn;
			y = yIn;
			xy = x * y;
			x2 = x * x;
			y2 = y * y;
			
			if( past != null) {
//				difx = x - past.x;
//				dify = y - past.y;
				difx = (x - past.x) / past.x;
				dify = (y - past.y) / past.y;
				difxy = difx * dify;
				difx2 = difx * difx;
				dify2 = dify * dify;
			}
		}
	}