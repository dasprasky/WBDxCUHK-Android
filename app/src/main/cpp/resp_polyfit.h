#ifndef _POLYFIT_H_
#define _POLYFIT_H_
#define POLYNOMIAL_MAX_ORDER    (10)

#define polyvals	_polyvals_respiration
#define polyfit		_polyfit_respiration

void polyvals(float x[], float y[], float a[], uint8_t N, uint8_t order);
void polyfit(float x[], float y[], float a[], uint8_t N, uint8_t order);
#endif