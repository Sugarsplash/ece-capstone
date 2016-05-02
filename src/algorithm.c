/*
This program will recieve the necessary sensor data and then compute the 
Reference evapotranspiration using the Hardgreaves-Reduced Set method.
The reference evapotranspiration will then be converted to the crop 
evapotranspiration.

Author: Bryan Martin
*/


/*
Necessary inputs:
D_y = Day of the year from 1-365
j = latitude in radians
T_max = Daytime high ambient air temp in celsius
T_min = Daytime low ambient air temp in celsius
*/

/*
Outline:
1)calculate d_r
2)calculate d
3)calculate omega_s
4)calculate R_a
5)calculate E_to
*/

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
//Constants used by HARG (can be calibrated)
#define HC  0.0023
#define HT  17.8
#define HE  0.424
#define pi 3.14159


//Constants used to convert E_to to E_tc
#define K_c  1
#define K_s  1


int main(int argc, char *argv[])
{
	if (argc != 5)
	{
		printf("Supply Day of year(1-365), latitude in radians, Daytime High, and Daytime Low.\n");
		return -1;
	}

	int D_y = atoi(argv[1]);
	int j = atoi(argv[2]);
	int T_max = atoi(argv[3]);
	int T_min = atoi(argv[4]);

	float d_r = 1 + 0.033 * cos(((2 * pi) / 365) * D_y);

	printf("DR = %2f\n", d_r);

	float d = 0.409 * sin(((2 * pi * D_y) / 365) - 1.39);

	printf("D = %2f\n", d);

	double omega_s = acos(-1 * tan(j) * tan(d));

	printf("Wiggly = %2f\n", omega_s);

	float R_a = ((24 * 60) / pi) * 0.082 * 0.408 * d_r * \
			(omega_s * sin(j) * sin(d) + (cos(j) * cos(d) * sin(omega_s)));

	printf("RA = %2f\n", R_a);

	float E_to = R_a * HC * pow((T_max - T_min), HE) * (((T_max + T_min) / 2) + HT);

	printf("ETO = %2f\n", E_to);

	float E_tc = E_to * K_s * K_c;

	printf("Crop evapotranspiration: %2f\n", E_tc);

	return 0;
}