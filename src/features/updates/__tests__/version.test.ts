import { compareVersions, isNewerVersion, parseVersion } from '@/features/updates/version';

describe('parseVersion', () => {
  it('strips a leading v and prerelease/build metadata', () => {
    expect(parseVersion('v1.2.3')).toEqual([1, 2, 3]);
    expect(parseVersion('1.2.3-beta.1')).toEqual([1, 2, 3]);
    expect(parseVersion('1.2')).toEqual([1, 2]);
  });
});

describe('compareVersions', () => {
  it('orders by numeric parts', () => {
    expect(compareVersions('1.2.0', '1.2.0')).toBe(0);
    expect(compareVersions('1.3.0', '1.2.9')).toBe(1);
    expect(compareVersions('1.2.0', '1.10.0')).toBe(-1);
  });
  it('treats missing parts as zero', () => {
    expect(compareVersions('1.2', '1.2.0')).toBe(0);
    expect(compareVersions('1.2.1', '1.2')).toBe(1);
  });
});

describe('isNewerVersion', () => {
  it('is true only when latest strictly beats current', () => {
    expect(isNewerVersion('0.2.0', '0.1.0')).toBe(true);
    expect(isNewerVersion('v0.1.1', '0.1.0')).toBe(true);
    expect(isNewerVersion('0.1.0', '0.1.0')).toBe(false);
    expect(isNewerVersion('0.1.0', '0.2.0')).toBe(false);
  });
});
